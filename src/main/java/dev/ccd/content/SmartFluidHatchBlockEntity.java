package dev.ccd.content;

import java.util.List;
import com.simibubi.create.content.logistics.itemHatch.HatchFilterSlot;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** Native fluid filter and hatch filter-slot interaction, without a tank. */
public final class SmartFluidHatchBlockEntity extends FluidHatchBlockEntity {
    public SmartFluidHatchBlockEntity(BlockPos pos, BlockState state) {
        super(CopperDynamics.SMART_FLUID_HATCH_BE.get(), pos, state);
    }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.add(filtering = new FilteringBehaviour(this, new HatchFilterSlot() {
            @Override public net.minecraft.world.phys.Vec3 getLocalOffset(net.minecraft.world.level.LevelAccessor level,
                BlockPos pos,BlockState state) {
                // Include the latest -0.14 voxel X shift on top of the existing alignment;
                // native testHit and the rendered filter both use this offset in every facing.
                var delta=net.createmod.catnip.math.VecHelper.rotate(
                    new net.minecraft.world.phys.Vec3(-.28/16,-2.30/16,.90/16),
                    net.createmod.catnip.math.AngleHelper.horizontalAngle(state.getValue(FluidHatchBlock.FACING)),
                    net.minecraft.core.Direction.Axis.Y);
                return super.getLocalOffset(level,pos,state).add(delta);
            }
        }).forFluids());
    }
    @Override public void clearContent() { filtering.setFilter(ItemStack.EMPTY); }
    @Override protected boolean acceptsFluid(FluidStack fluid) {
        // Native empty filters accept every fluid.
        return filtering != null && filtering.test(fluid);
    }
    @Override protected FluidStack drainAllowed(IFluidHandler target, int amount, FluidAction action) {
        // Unspecified drains must search all tanks, rather than draining and then discarding a rejected fluid.
        for (int tank = 0; tank < target.getTanks(); tank++) {
            FluidStack sample = target.getFluidInTank(tank).copy();
            if (sample.isEmpty() || !acceptsFluid(sample)) continue;
            FluidStack wanted = sample.copyWithAmount(Math.min(amount, sample.getAmount()));
            FluidStack planned = target.drain(wanted.copy(), FluidAction.SIMULATE);
            if (planned.isEmpty() || !FluidStack.isSameFluidSameComponents(wanted, planned) || !acceptsFluid(planned)) continue;
            if (action.simulate()) return planned;
            return target.drain(planned.copyWithAmount(Math.min(wanted.getAmount(), planned.getAmount())), action);
        }
        return FluidStack.EMPTY;
    }
}
