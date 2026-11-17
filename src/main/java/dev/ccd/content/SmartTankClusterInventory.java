package dev.ccd.content;

import java.util.ArrayList;
import java.util.List;
import dev.ccd.CCDConfig;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/** A live capability view; every actual mB remains owned and saved by exactly one member. */
public final class SmartTankClusterInventory implements IFluidHandler {
    public record Snapshot(List<SmartFluidTankBlockEntity> parts, List<FluidStack> fluids, long amount, int capacity, int kinds) {
        @Override public List<FluidStack> fluids() { return fluids.stream().map(FluidStack::copy).toList(); }
    }
    private final SmartFluidTankBlockEntity owner;
    public SmartTankClusterInventory(SmartFluidTankBlockEntity owner) { this.owner = owner; }

    @Nullable public Snapshot snapshot() {
        SmartFluidTankBlockEntity controller = owner.getControllerBE();
        if (controller == null) return null;
        List<SmartFluidTankBlockEntity> parts = controller.loadedParts();
        if (parts == null) return null;
        Snapshot cached = controller.cachedStorage;
        if (cached != null && cached.parts.equals(parts)) return cached;
        List<FluidStack> fluids = new ArrayList<>();
        long amount = 0;
        int kinds = 0;
        for (var part : parts) for (int slot = 0; slot < part.localInventory.getTanks(); slot++) {
            FluidStack fluid = part.localInventory.getFluidInTank(slot);
            if (fluid.isEmpty()) continue;
            amount += fluid.getAmount();
            boolean known = fluids.stream().anyMatch(stored -> FluidStack.isSameFluidSameComponents(stored, fluid));
            if (!known) kinds++;
            int left = fluid.getAmount();
            for (var stored : fluids) if (FluidStack.isSameFluidSameComponents(stored, fluid)) {
                int added = (int) Math.min(left, (long) Integer.MAX_VALUE - stored.getAmount());
                stored.grow(added);
                left -= added;
                if (left == 0) break;
            }
            if (left > 0) fluids.add(fluid.copyWithAmount(left));
        }
        int capacity = (int) Math.min(Integer.MAX_VALUE, (long) parts.size() * FluidTankBlockEntity.getCapacityMultiplier());
        return controller.cachedStorage = new Snapshot(List.copyOf(parts), List.copyOf(fluids), amount, capacity, kinds);
    }
    public long amount() { var view = snapshot(); return view == null ? 0 : view.amount; }
    public int capacity() { var view = snapshot(); return view == null ? 0 : view.capacity; }
    public int kinds() { var view = snapshot(); return view == null ? 0 : view.kinds; }
    @Override public int getTanks() {
        var view = snapshot();
        return view == null ? 0 : view.fluids.size() + (view.kinds < CCDConfig.SMART_TANK_MAX_FLUIDS.get() ? 1 : 0);
    }
    @Override public FluidStack getFluidInTank(int tank) {
        var view = snapshot();
        return view == null || tank < 0 || tank >= view.fluids.size() ? FluidStack.EMPTY : view.fluids.get(tank).copy();
    }
    @Override public int getTankCapacity(int tank) {
        var view = snapshot();
        if (view == null || tank < 0 || tank > view.fluids.size()) return 0;
        if (tank == view.fluids.size() && view.kinds >= CCDConfig.SMART_TANK_MAX_FLUIDS.get()) return 0;
        long stored = tank < view.fluids.size() ? view.fluids.get(tank).getAmount() : 0;
        return (int) Math.min(Integer.MAX_VALUE, stored + Math.max(0, view.capacity - view.amount));
    }
    @Override public boolean isFluidValid(int tank, FluidStack fluid) {
        var view = snapshot();
        if (view == null || fluid.isEmpty() || tank < 0 || tank > view.fluids.size()) return false;
        if (tank < view.fluids.size()) return FluidStack.isSameFluidSameComponents(view.fluids.get(tank), fluid);
        return view.kinds < CCDConfig.SMART_TANK_MAX_FLUIDS.get()
            && view.fluids.stream().noneMatch(stored -> FluidStack.isSameFluidSameComponents(stored, fluid));
    }
    @Override public int fill(FluidStack resource, FluidAction action) {
        var view = snapshot();
        var controller = owner.getControllerBE();
        if (view == null || controller == null || controller.storageBusy || resource.isEmpty()) return 0;
        boolean known = view.fluids.stream().anyMatch(fluid -> FluidStack.isSameFluidSameComponents(fluid, resource));
        if (!known && view.kinds >= CCDConfig.SMART_TANK_MAX_FLUIDS.get()) return 0;
        int limit = (int) Math.min(resource.getAmount(), Math.max(0, view.capacity - view.amount));
        if (limit == 0) return 0;
        controller.storageBusy = true;
        try {
            int accepted = 0;
            for (var part : view.parts) {
                accepted += part.localInventory.fillShared(resource.copyWithAmount(limit - accepted), action);
                if (accepted == limit) break;
            }
            return accepted;
        } finally { controller.storageBusy = false; }
    }
    @Override public FluidStack drain(FluidStack resource, FluidAction action) {
        var view = snapshot();
        var controller = owner.getControllerBE();
        if (view == null || controller == null || controller.storageBusy || resource.isEmpty()) return FluidStack.EMPTY;
        controller.storageBusy = true;
        try {
            int taken = 0;
            for (var part : view.parts) {
                taken += part.localInventory.drain(resource.copyWithAmount(resource.getAmount() - taken), action).getAmount();
                if (taken == resource.getAmount()) break;
            }
            return taken == 0 ? FluidStack.EMPTY : resource.copyWithAmount(taken);
        } finally { controller.storageBusy = false; }
    }
    @Override public FluidStack drain(int amount, FluidAction action) {
        var view = snapshot();
        if (view == null || amount <= 0 || view.fluids.isEmpty()) return FluidStack.EMPTY;
        return drain(view.fluids.getFirst().copyWithAmount(amount), action);
    }
}
