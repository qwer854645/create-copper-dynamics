package dev.ccd.content;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** Transfer boundary shared by the machine and in-game contract tests. */
public final class FluidTransfers {
    private FluidTransfers() {}

    public static FluidStack planDrain(IFluidHandler source, FluidStack filter, int amount, boolean exact) {
        if (amount <= 0) return FluidStack.EMPTY;
        for (int tank = 0; tank < source.getTanks(); tank++) {
            FluidStack candidate = source.getFluidInTank(tank).copy();
            if (candidate.isEmpty() || (!filter.isEmpty() && !FluidStack.isSameFluidSameComponents(candidate, filter))) continue;
            FluidStack result = source.drain(candidate.copyWithAmount(amount), FluidAction.SIMULATE);
            if (result.isEmpty() || !FluidStack.isSameFluidSameComponents(candidate, result)) continue;
            if (exact && result.getAmount() < amount) continue;
            return result.copyWithAmount(Math.min(result.getAmount(), amount));
        }
        return FluidStack.EMPTY;
    }

    public static boolean canFillAll(IFluidHandler target, FluidStack fluid) {
        return !fluid.isEmpty() && target.fill(fluid.copy(), FluidAction.SIMULATE) == fluid.getAmount();
    }

    /** Returns owned, undelivered fluid, even when execution differs from simulation. */
    public static FluidStack fillAndKeepRemainder(IFluidHandler target, FluidStack fluid) {
        if (fluid.isEmpty()) return FluidStack.EMPTY;
        int accepted = Math.clamp(target.fill(fluid.copy(), FluidAction.EXECUTE), 0, fluid.getAmount());
        return accepted == fluid.getAmount() ? FluidStack.EMPTY : fluid.copyWithAmount(fluid.getAmount() - accepted);
    }
}
