package dev.ccd.logistics;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import dev.ccd.CopperDynamics;
import dev.ccd.CCDConfig;
import dev.ccd.content.FluidArmBlockEntity;
import dev.ccd.content.FluidArmCargoItem;
import dev.ccd.content.FluidHatchBlock;
import dev.ccd.content.FluidHatchBlockEntity;
import dev.ccd.content.FluidPackageItem;
import dev.ccd.content.FluidCargo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;

/** One explicitly selected fluid face. An ItemStack carries real fluid through the native arm state machine. */
public final class FluidArmPoint extends ArmInteractionPoint {
    public static final int BATCH = 1_000;
    private final Direction face;
    public Direction face() { return face; }
    public FluidArmPoint(Level level, BlockPos pos, BlockState state, Direction face, Mode mode) {
        super(CopperDynamics.FLUID_ARM_POINT.get(), level, pos, state);
        this.face = face;
        this.mode = mode;
    }
    @Override public boolean isValid() { return handler() != null || fillingTarget(level,pos)!=null; }
    @Nullable public static com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour fillingTarget(Level level,BlockPos pos){
        if(!level.hasChunkAt(pos)||!dev.ccd.CCDFeatures.enabled(level.getBlockState(pos)))return null;
        return com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour.get(level,pos,
            com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TYPE);
    }
    /** Shared by selection and every capability lookup, including points restored from older saves. */
    public static boolean canTarget(Level level, BlockPos pos) {
        return level.hasChunkAt(pos) && (level.getBlockState(pos).getBlock() instanceof FluidHatchBlock
            || level.getBlockEntity(pos) instanceof com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity
            || CCDConfig.ARM_ALLOW_DIRECT_CONTAINER_INTERACTION.get());
    }
    @Nullable private IFluidHandler handler() {
        return canTarget(level, pos) ? interactionHandler(level, pos, face) : null;
    }
    /** Hatches are arm/handheld interfaces, not pipe or machine forwarding capabilities. */
    @Nullable public static IFluidHandler interactionHandler(Level level, BlockPos pos, Direction face) {
        if (!level.hasChunkAt(pos)) return null;
        if(!dev.ccd.CCDFeatures.enabled(level.getBlockState(pos)))return null;
        if (level.getBlockEntity(pos) instanceof FluidHatchBlockEntity hatch) return hatch.interactionHandler(face);
        var handler=level.getCapability(Capabilities.FluidHandler.BLOCK, pos, face);
        if(level.getBlockEntity(pos) instanceof dev.ccd.content.TankBlazeBurnerBlockEntity)return handler;
        if(level.getBlockEntity(pos) instanceof com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity burner){
            var fallback=new dev.ccd.compat.SuperLavaFuel(burner);
            if(handler==null)return fallback;
            return new IFluidHandler(){
                public int getTanks(){return handler.getTanks();}
                public FluidStack getFluidInTank(int tank){return handler.getFluidInTank(tank);}
                public int getTankCapacity(int tank){return handler.getTankCapacity(tank);}
                public boolean isFluidValid(int tank,FluidStack fluid){return handler.isFluidValid(tank,fluid)||fallback.isFluidValid(0,fluid);}
                public int fill(FluidStack fluid,FluidAction action){
                    // Preserve third-party fuel handlers and only fall back when they decline.
                    int accepted=handler.fill(fluid,FluidAction.SIMULATE);
                    return accepted>0?handler.fill(fluid,action):fallback.fill(fluid,action);
                }
                public FluidStack drain(int amount,FluidAction action){return FluidStack.EMPTY;}
                public FluidStack drain(FluidStack fluid,FluidAction action){return FluidStack.EMPTY;}
            };
        }
        return handler;
    }
    private boolean allowed(ArmBlockEntity arm) {
        return arm instanceof FluidArmBlockEntity && !arm.isRemoved() && level == arm.getLevel()
            && pos.closerThan(arm.getBlockPos(), ArmBlockEntity.getRange());
    }
    @Override protected Vec3 getInteractionPositionVector() {
        return Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5));
    }
    @Override protected Direction getInteractionDirection() { return face.getOpposite(); }
    @Override protected void serialize(CompoundTag tag, BlockPos anchor) {
        super.serialize(tag, anchor);
        tag.putInt("FluidFace", face.get3DDataValue());
    }
    @Override public int getSlotCount(ArmBlockEntity arm) {
        IFluidHandler handler = allowed(arm) ? handler() : null;
        return handler == null ? 0 : handler.getTanks();
    }
    @Override public ItemStack extract(ArmBlockEntity arm, int tank, boolean simulate) {
        return extract(arm, tank, BATCH, simulate);
    }
    @Override public ItemStack extract(ArmBlockEntity arm, int tank, int amount, boolean simulate) {
        IFluidHandler handler = allowed(arm) ? handler() : null;
        if (handler == null || tank < 0 || tank >= handler.getTanks() || amount <= 0) return ItemStack.EMPTY;
        FluidStack sample = handler.getFluidInTank(tank).copy();
        if (sample.isEmpty() || !((FluidArmBlockEntity)arm).acceptsFluid(sample)) return ItemStack.EMPTY;
        FluidStack fluid = handler.drain(sample.copyWithAmount(Math.min(amount, BATCH)),
            simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
        // Preserve even unexpected actual results; the native held stack owns the full payload.
        return FluidArmCargoItem.containing(fluid);
    }
    @Override public ItemStack insert(ArmBlockEntity arm, ItemStack parcel, boolean simulate) {
        if(allowed(arm)&&((FluidArmBlockEntity)arm).fillingMode())return fillItems((FluidArmBlockEntity)arm,parcel,simulate);
        IFluidHandler handler = allowed(arm) ? handler() : null;
        FluidStack fluid = FluidPackageItem.contents(parcel);
        if (handler == null || fluid.isEmpty()) return parcel.copy();
        int accepted = Math.clamp(handler.fill(fluid.copy(), simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE),
            0, fluid.getAmount());
        if (accepted == 0) return parcel.copy();
        if (accepted == fluid.getAmount()) return ItemStack.EMPTY;
        ItemStack rest = parcel.copyWithCount(1);
        rest.set(CopperDynamics.FLUID_CARGO.get(), new FluidCargo(fluid.copyWithAmount(fluid.getAmount() - accepted)));
        return rest;
    }
    private ItemStack fillItems(FluidArmBlockEntity arm,ItemStack parcel,boolean simulate){
        var target=fillingTarget(level,pos);var fluid=FluidPackageItem.contents(parcel);
        if(target==null||fluid.isEmpty())return parcel.copy();
        int[] used={0};
        target.handleProcessingOnAllItems(item->{
            if(used[0]>0)return null;
            int amount=com.simibubi.create.content.fluids.spout.FillingBySpout.getRequiredAmountForItem(level,item.stack.copy(),fluid.copy());
            if(amount<=0||amount>fluid.getAmount()||!arm.acceptsFilling(item.stack,fluid,amount))return null;
            if(simulate){used[0]=amount;return null;}
            var input=item.stack.copy();var available=fluid.copy();
            var result=com.simibubi.create.content.fluids.spout.FillingBySpout.fillItem(level,amount,input,available);
            if(input.getCount()!=item.stack.getCount()-1||fluid.getAmount()-available.getAmount()!=amount)return null;
            var output=item.copy();output.stack=result;
            var remainder=item.copy();remainder.stack=input;used[0]=amount;
            return com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult.convertToAndLeaveHeld(
                result.isEmpty()?java.util.List.of():java.util.List.of(output),input.isEmpty()?null:remainder);
        });
        if(used[0]==0)return parcel.copy();
        if(used[0]==fluid.getAmount())return ItemStack.EMPTY;
        var rest=parcel.copyWithCount(1);rest.set(CopperDynamics.FLUID_CARGO.get(),new FluidCargo(fluid.copyWithAmount(fluid.getAmount()-used[0])));return rest;
    }
    public static final class Type extends ArmInteractionPointType {
        // The native item-arm picker must not select fluid-only targets. FluidArmBlockEntity
        // loads these explicitly, with a side, range and type check, instead of the item loader.
        @Override public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) { return false; }
        @Override public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
            return new FluidArmPoint(level, pos, state, Direction.UP, Mode.DEPOSIT);
        }
    }
}
