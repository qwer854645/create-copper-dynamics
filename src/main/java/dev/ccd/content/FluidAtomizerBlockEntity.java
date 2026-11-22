package dev.ccd.content;

import java.util.List;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.fluids.FluidFX;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import dev.ccd.CopperDynamics;
import dev.ccd.CCDTooltip;
import dev.ccd.api.AtomizerScripts;
import dev.ccd.atomizing.AtomizerProcessing;
import dev.ccd.atomizing.AtomizingRecipe;
import dev.ccd.atomizing.AtomizingRules;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** Native fan plus a paid medium, not a separate item-processing or airflow implementation. */
public final class FluidAtomizerBlockEntity extends EncasedFanBlockEntity {
    public static final int CAPACITY = 4000;
    private SmartFluidTankBehaviour tank;
    private FluidStack lookupFluid = FluidStack.EMPTY, activeFluid = FluidStack.EMPTY;
    private AtomizingRules.Selection selected, active;
    private FanProcessingType clientType;
    private long lookupRevision = -1, activeRevision = -1, paidUntil, nextEntityPulse;
    private boolean mistActive;
    private String processingContext = "";
    private com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour outputFilter;
    private long filterRevision;

    public FluidAtomizerBlockEntity(BlockPos pos, BlockState state) {
        super(CopperDynamics.FLUID_ATOMIZER_BE.get(), pos, state);
        airCurrent = new MistAirCurrent(this);
    }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.add(tank = SmartFluidTankBehaviour.single(this, CAPACITY));
        behaviours.add(outputFilter = new com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour(this,
            new com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided() {
                @Override protected Vec3 getSouthLocation() { return new Vec3(.5,.5,1.01); }
                @Override protected boolean isSideActive(BlockState state,Direction side) {
                    return side.getAxis()!=state.getValue(FluidAtomizerBlock.FACING).getAxis();
                }
            }).withCallback(stack -> { filterRevision++; updateAirFlow=true; }));
        outputFilter.setLabel(Component.translatable("ccd.atomizer.output_filter"));
    }
    public boolean hasOutputFilter() { return outputFilter!=null&&!outputFilter.getFilter().isEmpty(); }
    public boolean acceptsOutput(ItemStack stack) { return !hasOutputFilter()||!stack.isEmpty()&&outputFilter.test(stack); }
    public boolean acceptsRecipe(net.minecraft.world.item.crafting.Recipe<?> recipe) {
        if(!hasOutputFilter())return true;
        if(recipe instanceof com.simibubi.create.content.processing.recipe.ProcessingRecipe<?,?> processing)
            return processing.getRollableResults().stream().anyMatch(result->result.getChance()>0&&acceptsOutput(result.getStack()));
        return acceptsOutput(recipe.getResultItem(level.registryAccess()));
    }
    @Override public void initialize() {
        super.initialize();
        FluidAtomizerBlock.updateSignal(level, worldPosition);
    }
    public boolean isFluidSide(Direction side) { return side == null || side.getAxis() != getBlockState().getValue(FluidAtomizerBlock.FACING).getAxis(); }
    public IFluidHandler fluidHandler(Direction side) { return isFluidSide(side) ? tank.getCapability() : null; }
    public boolean mistActive() { return mistActive; }
    public FluidStack displayFluid() { return tank.getPrimaryHandler().getFluid().copy(); }
    public FluidStack activeFluid() { return activeFluid.copy(); }
    private FanProcessingType activeType() { return level != null && level.isClientSide() ? clientType : active == null ? null : active.type(); }
    public FanProcessingType nativeFallback() {
        if (active == null || active.recipe() == null || active.recipe().processingType().isEmpty()) return null;
        var type = AtomizingRules.parseType(active.recipe().processingType());
        return type == CopperDynamics.ATOMIZING_FAN_TYPE.get() ? null : type;
    }
    public AtomizingRecipe.ItemRule itemRule(ItemStack stack) {
        if (active != null && active.recipe() != null)
            for (var item : active.recipe().items()) if (item.input().test(stack)) return item;
        return null;
    }
    public String processingKey(ItemStack stack) {
        int index = active == null || active.recipe() == null ? -1 : active.recipe().items().indexOf(itemRule(stack));
        return processingContext + ":" + index + ":" + filterRevision;
    }
    @Override public void tick() {
        if (level != null && !level.isClientSide()) prepareMist();
        super.tick();
        if (level != null && level.isClientSide() && mistActive && level.getGameTime() % 5 == 0 && airCurrent.maxDistance > .25f) {
            Vec3 offset = Vec3.atLowerCornerOf(getAirflowOriginSide().getNormal());
            Vec3 pos = Vec3.atCenterOf(worldPosition).add(offset.scale(.7));
            level.addParticle(FluidFX.getFluidParticle(activeFluid), pos.x, pos.y, pos.z, offset.x * .08, offset.y * .08, offset.z * .08);
        }
    }
    private void prepareMist() {
        if (getSpeed() == 0 || getBlockState().getValue(FluidAtomizerBlock.POWERED)) { stopMist(); return; }
        if (activeRevision == AtomizingRules.revision() && active != null && active.recipe() != null && Math.abs(getSpeed()) < active.recipe().minimumRpm()) { stopMist(); return; }
        FluidStack available = tank.getPrimaryHandler().getFluid();
        if (mistActive && !available.isEmpty() && !FluidStack.isSameFluidSameComponents(available, activeFluid)) stopMist();
        if (mistActive && activeRevision == AtomizingRules.revision() && level.getGameTime() < paidUntil) return;
        if (lookupRevision != AtomizingRules.revision() || !FluidStack.isSameFluidSameComponents(available, lookupFluid)) {
            lookupFluid = available.copy();
            lookupRevision = AtomizingRules.revision();
            selected = AtomizingRules.select(level, available);
        }
        if (available.isEmpty() || selected == null || !selected.hasAction()
            || selected.recipe() != null && Math.abs(getSpeed()) < selected.recipe().minimumRpm()
            || available.getAmount() < selected.amount()) { stopMist(); return; }
        FluidStack paid = tank.getPrimaryHandler().drain(selected.amount(), FluidAction.EXECUTE);
        if (paid.getAmount() != selected.amount()) { stopMist(); return; }
        boolean changed = !mistActive || active != selected || !FluidStack.isSameFluidSameComponents(activeFluid, paid);
        mistActive = true;
        active = selected;
        activeRevision = lookupRevision;
        activeFluid = paid;
        if (changed) processingContext = active.id() + ":" + activeRevision + ":"
            + BuiltInRegistries.FLUID.getKey(activeFluid.getFluid()) + ":" + activeFluid.getComponentsPatch();
        paidUntil = level.getGameTime() + selected.interval();
        if (changed) { nextEntityPulse = level.getGameTime(); updateAirFlow = true; sendData(); }
    }
    private void stopMist() {
        boolean changed = mistActive;
        mistActive = false;
        active = null;
        activeFluid = FluidStack.EMPTY;
        paidUntil = 0;
        if (changed) { updateAirFlow = true; sendData(); }
    }
    private void pulseEntities(List<net.minecraft.world.entity.Entity> entities) {
        if (level.isClientSide() || !mistActive || active == null || level.getGameTime() < nextEntityPulse) return;
        nextEntityPulse = level.getGameTime() + (active.recipe() == null ? 20 : active.recipe().entityInterval());
        for (var entity : entities) if (entity instanceof LivingEntity living && entity.isAlive() && entity.getBoundingBox().intersects(airCurrent.bounds)) {
            dev.ccd.compat.FluidWorldEffects.applyMist(living,activeFluid.getFluidType());
            var recipe = active.recipe();
            if (recipe != null && !recipe.entityTypes().isEmpty() && !recipe.entityTypes().contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))) continue;
            AtomizerScripts.EntityResult scripted = null;
            if (recipe != null && !recipe.entityScript().isEmpty()) {
                scripted = AtomizerScripts.processEntity(recipe.entityScript(), new AtomizerScripts.EntityContext(
                    BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()), entity.getUUID(), living.getHealth(), activeFluid,
                    getSpeed(), worldPosition, level.dimension().location()));
                if (scripted == null) continue;
            }
            if (active.potion()) for (var effect : activeFluid.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getAllEffects()) {
                applyEffect(living, mistPotionEffect(effect), 1);
            }
            if (recipe != null) {
                for (var effect : recipe.effects()) applyEffect(living, effect, .5);
                if (recipe.damage() > 0) living.hurt(level.damageSources().magic(), recipe.damage());
            }
            if (scripted != null) {
                for (var effect : scripted.effects()) applyEffect(living, effect, .5);
                if (scripted.damage() > 0) living.hurt(level.damageSources().magic(), scripted.damage());
            }
        }
    }
    public static MobEffectInstance mistPotionEffect(MobEffectInstance effect){
        int duration=effect.isInfiniteDuration()?600:Math.max(1,Math.min(600,effect.getDuration()/4));
        return new MobEffectInstance(effect.getEffect(),duration,Math.min(255,effect.getAmplifier()+1),effect.isAmbient(),effect.isVisible(),effect.showIcon());
    }
    private void applyEffect(LivingEntity target, MobEffectInstance effect, double strength) {
        if (effect.getEffect().value().isInstantenous()) effect.getEffect().value().applyInstantenousEffect(null, null, target, effect.getAmplifier(), strength);
        else target.addEffect(new MobEffectInstance(effect));
    }
    @Override public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("MistActive", mistActive);
        tag.put("MistFluid", activeFluid.saveOptional(registries));
        var type = activeType();
        var id = type == null ? null : CreateBuiltInRegistries.FAN_PROCESSING_TYPE.getKey(type);
        tag.putString("MistType", id == null ? "" : id.toString());
    }
    @Override protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        mistActive = clientPacket && tag.getBoolean("MistActive");
        activeFluid = mistActive ? FluidStack.parseOptional(registries, tag.getCompound("MistFluid")) : FluidStack.EMPTY;
        clientType = tag.getString("MistType").isEmpty() ? null : FanProcessingType.parse(tag.getString("MistType"));
        if (clientPacket) airCurrent.rebuild();
        else { active = null; selected = null; paidUntil = 0; lookupRevision = -1; }
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        super.addToGoggleTooltip(tooltip, sneaking);
        containedFluidTooltip(tooltip, sneaking, tank.getCapability());
        CCDTooltip.heading(tooltip, "ccd.gui.goggles.atomizer");
        CCDTooltip.line(tooltip, mistActive ? ChatFormatting.AQUA : ChatFormatting.GRAY,
            mistActive ? "ccd.atomizer.active" : "ccd.atomizer.idle");
        return true;
    }
    private static final class MistAirCurrent extends AirCurrent {
        private final FluidAtomizerBlockEntity owner;
        private MistAirCurrent(FluidAtomizerBlockEntity owner) { super(owner); this.owner = owner; }
        @Override public FanProcessingType getTypeAt(float offset) {
            var nativeType = super.getTypeAt(offset);
            return nativeType != null ? nativeType : owner.mistActive && offset >= 0 && offset <= maxDistance ? owner.activeType() : null;
        }
        @Override protected void tickAffectedEntities(Level world) {
            AtomizerProcessing.withSource(owner, () -> super.tickAffectedEntities(world));
            owner.pulseEntities(caughtEntities);
        }
        @Override public void tickAffectedHandlers() {
            AtomizerProcessing.withSource(owner, () -> super.tickAffectedHandlers());
        }
    }
}
