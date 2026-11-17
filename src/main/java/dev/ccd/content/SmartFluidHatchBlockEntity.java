package dev.ccd.content;

import java.util.List;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.content.logistics.itemHatch.HatchFilterSlot;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import dev.ccd.CopperDynamics;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
        behaviours.add(filtering = new FilteringBehaviour(this, new HatchFilterSlot()).forFluids()
            .withCallback(this::filterChanged));
    }
    @Override public void clearContent() { filtering.setFilter(ItemStack.EMPTY); }
    @Override protected boolean acceptsFluid(FluidStack fluid) {
        // Deliberately fail closed until configured; native empty filters otherwise accept everything.
        return filtering != null && !filtering.getFilter().isEmpty() && filtering.test(fluid);
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
    private void filterChanged(ItemStack stack) {
        if (level == null || level.isClientSide()) return;
        invalidateCapabilities();
        var front = getBlockState().getValue(FluidHatchBlock.FACING).getOpposite();
        BlockPos neighbour = worldPosition.relative(front);
        if (!level.hasChunkAt(neighbour)) return;
        if (level.getBlockEntity(neighbour) instanceof PumpBlockEntity pump) pump.updatePressureChange();
        else FluidPropagator.propagateChangedPipe(level, neighbour, level.getBlockState(neighbour));
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        super.addToGoggleTooltip(tooltip, sneaking);
        tooltip.add(Component.translatable(filtering.getFilter().isEmpty()
            ? "ccd.smart_fluid_hatch.unconfigured" : "ccd.smart_fluid_hatch.filtered").withStyle(ChatFormatting.GRAY));
        return true;
    }
}
