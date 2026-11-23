package dev.ccd.content;

import java.util.ArrayList;
import java.util.List;
import dev.ccd.CCDConfig;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/** Live member capability forwarding to the controller's one authoritative inventory. */
public final class SmartTankClusterInventory implements IFluidHandler {
    public record Snapshot(List<FluidStack> fluids, long amount, int capacity, int kinds, int blocks) {
        @Override public List<FluidStack> fluids() { return fluids.stream().map(FluidStack::copy).toList(); }
    }
    private final SmartFluidTankBlockEntity owner;
    public SmartTankClusterInventory(SmartFluidTankBlockEntity owner) { this.owner = owner; }

    @Nullable public Snapshot snapshot() {
        if (SmartFluidTankBlockEntity.isRestructuring()) return null;
        var level = owner.getLevel();
        if (level == null || owner.isRemoved() || !level.hasChunkAt(owner.getBlockPos())
            || level.getBlockEntity(owner.getBlockPos()) != owner) return null;
        SmartFluidTankBlockEntity controller = owner.getControllerBE();
        if (controller == null) return null;
        Snapshot cached = controller.cachedStorage;
        if (cached != null) return cached;
        List<FluidStack> fluids = new ArrayList<>();
        for (int slot = 0; slot < controller.storedInventory.getTanks(); slot++) {
            FluidStack fluid = controller.storedInventory.getFluidInTank(slot);
            if (!fluid.isEmpty()) fluids.add(fluid);
        }
        return controller.cachedStorage = new Snapshot(List.copyOf(fluids), controller.storedInventory.amount(),
            controller.storedInventory.capacity(), controller.storedInventory.kinds(), controller.getTotalTankSize());
    }
    public long amount() { var view = snapshot(); return view == null ? 0 : view.amount; }
    public int capacity() { var view = snapshot(); return view == null ? 0 : view.capacity; }
    public int kinds() { var view = snapshot(); return view == null ? 0 : view.kinds; }
    /** Independent planning state with the same shared capacity and fluid-kind limit. */
    @Nullable public SmartTankInventory planningCopy() {
        var view = snapshot();
        if (view == null) return null;
        var copy = new SmartTankInventory(() -> view.capacity(), () -> CCDConfig.SMART_TANK_MAX_FLUIDS.get(), () -> {});
        copy.absorbOwned(view.fluids());
        return copy;
    }
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
        controller.storageBusy = true;
        try {
            return controller.storedInventory.fill(resource.copy(), action);
        } finally { controller.storageBusy = false; }
    }
    @Override public FluidStack drain(FluidStack resource, FluidAction action) {
        var view = snapshot();
        var controller = owner.getControllerBE();
        if (view == null || controller == null || controller.storageBusy || resource.isEmpty()) return FluidStack.EMPTY;
        controller.storageBusy = true;
        try {
            return controller.storedInventory.drain(resource.copy(), action);
        } finally { controller.storageBusy = false; }
    }
    @Override public FluidStack drain(int amount, FluidAction action) {
        var view = snapshot();
        if (view == null || amount <= 0 || view.fluids.isEmpty()) return FluidStack.EMPTY;
        return drain(view.fluids.getFirst().copyWithAmount(amount), action);
    }
}
