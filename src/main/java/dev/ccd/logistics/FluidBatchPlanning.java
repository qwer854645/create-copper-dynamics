package dev.ccd.logistics;

import dev.ccd.content.SmartTankClusterInventory;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Batch preflight without mutating a real target. CCD shared storage has an exact copy. */
public final class FluidBatchPlanning {
    private FluidBatchPlanning() {}
    public static boolean fits(Level level, BlockPos pos, Direction side, List<FluidStack> fluids) {
        var sources = FluidAccess.resolve(level, pos, side);
        List<IFluidHandler> plans = new ArrayList<>();
        for (var source : sources) {
            if (source.handler() instanceof SmartTankClusterInventory smart) plans.add(smart.planningCopy());
            else {
                var tanks = new ArrayList<FluidTank>();
                for (int i = 0; i < source.handler().getTanks(); i++) {
                    final int slot = i;
                    var tank = new FluidTank(source.handler().getTankCapacity(i), fluid -> source.handler().isFluidValid(slot, fluid));
                    tank.setFluid(source.handler().getFluidInTank(i).copy());
                    tanks.add(tank);
                }
                plans.add(new net.neoforged.neoforge.fluids.capability.templates.FluidTank(0) {
                    @Override public int fill(FluidStack fluid, FluidAction action) {
                        int remaining = fluid.getAmount();
                        for (var tank : tanks) remaining -= tank.fill(fluid.copyWithAmount(remaining), action);
                        return fluid.getAmount() - remaining;
                    }
                });
            }
        }
        for (var fluid : fluids) {
            int left = fluid.getAmount();
            for (int i = 0; i < sources.size() && left > 0; i++) {
                var source = sources.get(i);
                var plan = plans.get(i);
                if (plan == null || !source.filter().test(fluid)) continue;
                int allowed = Math.clamp(source.handler().fill(fluid.copyWithAmount(left), IFluidHandler.FluidAction.SIMULATE), 0, left);
                left -= plan.fill(fluid.copyWithAmount(allowed), IFluidHandler.FluidAction.EXECUTE);
            }
            if (left != 0) return false;
        }
        return true;
    }
}
