package dev.ccd.content;

import java.util.List;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import dev.ccd.CopperDynamics;
import dev.ccd.CCDTooltip;
import dev.ccd.logistics.FluidArmPoint;
import dev.ccd.mixin.FluidArmAccessor;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Reuse the native arm phases, output policy, movement, redstone lock, held cargo and save format. */
public final class FluidArmBlockEntity extends ArmBlockEntity {
    public static final int MAX_POINTS = 64;
    private com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour fluidFilter;
    private boolean fillingMode;
    public boolean fillingMode(){return fillingMode;}
    public void setFillingMode(boolean value){fillingMode=value;notifyUpdate();}
    public boolean acceptsFluid(net.neoforged.neoforge.fluids.FluidStack fluid){return fillingMode||fluidFilter.test(fluid);}
    public boolean acceptsFilling(ItemStack item,net.neoforged.neoforge.fluids.FluidStack fluid,int required){
        return fluidFilter.getFilter().isEmpty()||dev.ccd.logistics.FillingOutputFilter.accepts(level,item,fluid,required,fluidFilter::test);
    }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours){
        super.addBehaviours(behaviours);
        // Keep the native scheduling behaviour and persistence, restricting its
        // existing transform to the north/south pair on this CCD arm only.
        var nativeSlot=selectionMode.getSlotPositioning();
        behaviours.remove(selectionMode);
        behaviours.add(selectionMode=new com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour<>(
            SelectionMode.class,Component.translatable("create.logistics.when_multiple_outputs_available"),this,
            new com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided(){
                @Override protected net.minecraft.world.phys.Vec3 getSouthLocation(){return new net.minecraft.world.phys.Vec3(.5,3/16d,15.5/16d);}
                @Override public net.minecraft.world.phys.Vec3 getLocalOffset(net.minecraft.world.level.LevelAccessor world,BlockPos pos,BlockState state){
                    return ((com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided)nativeSlot)
                        .fromSide(getSide()).getLocalOffset(world,pos,state);
                }
                @Override protected boolean isSideActive(BlockState state,Direction side){return side.getAxis()==Direction.Axis.Z;}
            }));
        behaviours.add(fluidFilter=new com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour(this,
            new com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided(){
                @Override protected net.minecraft.world.phys.Vec3 getSouthLocation(){return new net.minecraft.world.phys.Vec3(.5,3/16d,1.01);}
                @Override public net.minecraft.world.phys.Vec3 getLocalOffset(net.minecraft.world.level.LevelAccessor world,BlockPos pos,BlockState state){
                    var offset=super.getLocalOffset(world,pos,state);
                    return state.getValue(com.simibubi.create.content.kinetics.mechanicalArm.ArmBlock.CEILING)?new net.minecraft.world.phys.Vec3(offset.x,13/16d,offset.z):offset;
                }
                @Override protected boolean isSideActive(BlockState state,Direction side){return side.getAxis()==Direction.Axis.X;}
            }){
                @Override public boolean acceptsValueSettings(){return true;}
                @Override public boolean isCountVisible(){return false;}
                @Override public com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard createBoard(Player player,net.minecraft.world.phys.BlockHitResult hit){
                    return new com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard(Component.translatable("ccd.fluid_arm.filter_mode"),0,1,
                        List.of(Component.translatable("ccd.fluid_arm.transfer_mode"),Component.translatable("ccd.fluid_arm.filling_mode")),
                        new com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter(v->Component.translatable(v.row()==0?"ccd.fluid_arm.transfer_mode":"ccd.fluid_arm.filling_mode")));
                }
                @Override public ValueSettings getValueSettings(){return new ValueSettings(fillingMode?1:0,0);}
                @Override public void setValueSettings(Player player,ValueSettings value,boolean ctrl){setFillingMode(value.row()==1);}
                @Override public net.minecraft.network.chat.MutableComponent getLabel(){return Component.translatable(fillingMode?"ccd.smart_faucet.filter_output":"ccd.smart_faucet.filter_fluid");}
            });
    }
    public FluidArmBlockEntity(BlockPos pos, BlockState state) { super(CopperDynamics.FLUID_ARM_BE.get(), pos, state); }
    private FluidArmAccessor state() { return (FluidArmAccessor) (Object) this; }
    @Override public void initialize() {
        super.initialize();
        if (level != null && !level.isClientSide()) redstoneUpdate();
    }
    @Override protected void initInteractionPoints() {
        var state = state();
        if (!state.ccd$pointsDirty() || state.ccd$pointTag() == null
            || !isAreaActuallyLoaded(worldPosition, getRange() + 1)) return;
        state.ccd$inputs().clear();
        state.ccd$outputs().clear();
        ListTag tags = state.ccd$pointTag();
        for (int i = 0; i < Math.min(tags.size(), MAX_POINTS); i++) {
            CompoundTag tag = tags.getCompound(i);
            if (!tag.getString("Type").equals("ccd:fluid_arm_point")
                || !tag.contains("Pos", Tag.TAG_COMPOUND)) continue;
            BlockPos relative = NBTHelper.readBlockPos(tag, "Pos");
            if (!relative.closerThan(BlockPos.ZERO, getRange())) continue;
            BlockPos target = worldPosition.offset(relative);
            int side = tag.getInt("FluidFace");
            if (side < 0 || side >= Direction.values().length || !level.hasChunkAt(target)) continue;
            var mode = NBTHelper.readEnum(tag, "Mode", ArmInteractionPoint.Mode.class);
            var point = new FluidArmPoint(level, target, level.getBlockState(target), Direction.from3DDataValue(side), mode);
            (mode == ArmInteractionPoint.Mode.TAKE ? state.ccd$inputs() : state.ccd$outputs()).add(point);
        }
        state.ccd$pointsDirty(false);
        if (!level.isClientSide()) { setChanged(); sendData(); }
    }
    @Override protected int getDistributableAmount(ArmInteractionPoint input, int tank) {
        if (!(input instanceof FluidArmPoint)) return 0;
        ItemStack planned = input.extract(this, tank, true);
        int offered = FluidPackageItem.contents(planned).getAmount();
        if (offered <= 0) return 0;
        int accepted = 0;
        // Use the largest single-target acceptance, not a sum of overlapping capability views.
        for (var output : state().ccd$outputs()) {
            if (!(output instanceof FluidArmPoint) || output.getPos().equals(input.getPos()) || !output.isValid()) continue;
            ItemStack rest = output.insert(this, planned.copy(), true);
            accepted = Math.max(accepted, offered - FluidPackageItem.contents(rest).getAmount());
        }
        return Math.clamp(accepted, 0, FluidArmPoint.BATCH);
    }
    @Override protected void collectItem() {
        super.collectItem();
        // Native item extraction assumes a positive simulation produces a nonempty stack.
        // Fluid capabilities may change between simulation and execution; never wait with empty cargo.
        if (state().ccd$held().isEmpty() && state().ccd$phase() == Phase.SEARCH_OUTPUTS) {
            resetToInputs();
            notifyUpdate();
        }
    }
    public boolean wearingGoggles() { return state().ccd$goggles(); }
    public boolean unloadTo(Player player) {
        var state = state();
        if (state.ccd$held().isEmpty()) return false;
        if (level == null || level.isClientSide()) return true;
        ItemStack sealed = FluidArmCargoItem.sealed(state.ccd$held());
        state.ccd$held(ItemStack.EMPTY);
        resetToInputs();
        notifyUpdate();
        // Native inventory handling drops the remainder safely if the inventory is full.
        player.getInventory().placeItemBackInInventory(sealed);
        return true;
    }
    private void resetToInputs() {
        var state = state();
        state.ccd$phase(Phase.SEARCH_INPUTS);
        state.ccd$target(-1);
        state.ccd$progress(0);
    }
    @Override public void destroy() {
        state().ccd$held(FluidArmCargoItem.sealed(state().ccd$held()));
        super.destroy();
        state().ccd$held(ItemStack.EMPTY);
    }
    public void configure(List<FluidArmPoint> points) {
        ListTag tags = new ListTag();
        for (var point : points) {
            if (tags.size() >= MAX_POINTS) break;
            if (point.getPos().closerThan(worldPosition, getRange()) && point.isValid()) tags.add(point.serialize(worldPosition));
        }
        var state = state();
        state.ccd$pointTag(tags);
        state.ccd$pointsDirty(true);
        state.ccd$phase(state.ccd$held().isEmpty() ? Phase.SEARCH_INPUTS : Phase.SEARCH_OUTPUTS);
        state.ccd$target(-1);
        state.ccd$progress(0);
        lastInputIndex = lastOutputIndex = -1;
        notifyUpdate();
    }
    @Override protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        fillingMode=tag.getBoolean("FillingMode");
        state().ccd$held(FluidArmCargoItem.inClaw(state().ccd$held()));
        if (state().ccd$held().isEmpty() && state().ccd$phase() == Phase.SEARCH_OUTPUTS) resetToInputs();
        state().ccd$pointsDirty(true);
    }
    @Override public void transform(BlockEntity be, StructureTransform transform) {
        CompoundTag saved = new CompoundTag();
        writeInteractionPoints(saved);
        ListTag tags = saved.getList("InteractionPoints", Tag.TAG_COMPOUND);
        for (int i = 0; i < tags.size(); i++) {
            CompoundTag tag = tags.getCompound(i);
            ArmInteractionPoint.transformPos(tag, transform);
            Direction side = Direction.from3DDataValue(tag.getInt("FluidFace"));
            if (transform.mirror != null) side = transform.mirror.mirror(side);
            tag.putInt("FluidFace", transform.rotateFacing(side).get3DDataValue());
        }
        state().ccd$pointTag(tags);
        state().ccd$pointsDirty(true);
        notifyUpdate();
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        var nativeTooltip=new java.util.ArrayList<Component>();
        super.addToGoggleTooltip(nativeTooltip,sneaking);
        for(var line:nativeTooltip)tooltip.add(fluidArmHint(line));
        CCDTooltip.heading(tooltip, "ccd.gui.goggles.fluid_arm");
        var fluid = FluidPackageItem.contents(state().ccd$held());
        if (!fluid.isEmpty()) {
            com.simibubi.create.foundation.utility.CreateLang.fluidName(fluid).style(ChatFormatting.GRAY).forGoggles(tooltip,1);
            com.simibubi.create.foundation.utility.CreateLang.builder().add(CCDTooltip.amount(fluid.getAmount())).forGoggles(tooltip,1);
        }
        String status = workStatus();
        CCDTooltip.line(tooltip, status.equals("redstone") ? ChatFormatting.YELLOW : ChatFormatting.GRAY,
            "ccd.fluid_arm.status." + status);
        return true;
    }
    @Override public boolean addToTooltip(List<Component> tooltip, boolean sneaking) {
        // Retain native kinetic diagnostics, replacing only the item-arm hint.
        var nativeTooltip = new java.util.ArrayList<Component>();
        boolean shown = super.addToTooltip(nativeTooltip, sneaking);
        for (Component line : nativeTooltip) tooltip.add(fluidArmHint(line));
        return shown;
    }
    private static Component fluidArmHint(Component component) {
        var content = component.getContents();
        var result = content instanceof net.minecraft.network.chat.contents.TranslatableContents title
            && title.getKey().equals("create.hint.mechanical_arm_no_targets.title")
            ? component.copy().plainCopy().withStyle(component.getStyle())
            : content instanceof net.minecraft.network.chat.contents.TranslatableContents translated
            && translated.getKey().equals("create.hint.mechanical_arm_no_targets")
            ? Component.translatable("ccd.hint.fluid_arm_no_targets").withStyle(component.getStyle())
            : component.copy().plainCopy().withStyle(component.getStyle());
        for (Component sibling : component.getSiblings()) result.append(fluidArmHint(sibling));
        return result;
    }
    private String workStatus() {
        if (getSpeed() == 0) return "stopped";
        if (!state().ccd$held().isEmpty()) {
            return state().ccd$phase() == Phase.MOVE_TO_OUTPUT ? "delivering" : "waiting_output";
        }
        if (redstoneLocked) return "redstone";
        return state().ccd$phase() == Phase.MOVE_TO_INPUT ? "collecting" : "waiting_input";
    }
    @Override public void tick() {
        if (!dev.ccd.CCDFeatures.enabled(getBlockState())) return;
        super.tick();
    }
    @Override public void write(CompoundTag tag,HolderLookup.Provider registries,boolean packet){
        super.write(tag,registries,packet);tag.putBoolean("FillingMode",fillingMode);
    }
}
