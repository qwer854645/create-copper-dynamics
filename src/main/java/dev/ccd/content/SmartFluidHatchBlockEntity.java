package dev.ccd.content;

import java.util.List;
import com.simibubi.create.content.logistics.itemHatch.HatchFilterSlot;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import dev.ccd.CopperDynamics;
import dev.ccd.CCDTooltip;
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
        behaviours.add(filtering = new FilteringBehaviour(this, new HatchFilterSlot()).forFluids());
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
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        super.addToGoggleTooltip(tooltip, sneaking);
        CCDTooltip.line(tooltip, filtering.getFilter().isEmpty() ? ChatFormatting.YELLOW : ChatFormatting.GRAY,
            filtering.getFilter().isEmpty() ? "ccd.smart_fluid_hatch.unconfigured" : "ccd.smart_fluid_hatch.filtered");
        return true;
    }
}
