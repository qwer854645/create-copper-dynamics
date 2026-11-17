package dev.ccd.content;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ListTag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Stable virtual slots sharing one physical capacity. Fluid components are part of identity. */
public final class SmartTankInventory implements IFluidHandler {
    private final List<FluidStack> contents = new ArrayList<>();
    private final IntSupplier capacity, maxTypes;
    private final Runnable changed;
    public SmartTankInventory(IntSupplier capacity, IntSupplier maxTypes, Runnable changed) {
        this.capacity = capacity;
        this.maxTypes = maxTypes;
        this.changed = changed;
    }
    public int capacity() { return capacity.getAsInt(); }
    public long amount() { return contents.stream().mapToLong(FluidStack::getAmount).sum(); }
    public int kinds() {
        List<FluidStack> identities = new ArrayList<>();
        for (var fluid : contents) if (!fluid.isEmpty()
            && identities.stream().noneMatch(known -> FluidStack.isSameFluidSameComponents(known, fluid))) identities.add(fluid);
        return identities.size();
    }
    private int free() { return (int) Math.max(0, (long) capacity() - amount()); }
    @Override public int getTanks() { return Math.max(maxTypes.getAsInt(), contents.size()); }
    @Override public FluidStack getFluidInTank(int tank) {
        return tank < 0 || tank >= contents.size() ? FluidStack.EMPTY : contents.get(tank).copy();
    }
    @Override public int getTankCapacity(int tank) {
        if (tank < 0 || tank >= getTanks()) return 0;
        FluidStack stored = getFluidInTank(tank);
        if (stored.isEmpty() && kinds() >= maxTypes.getAsInt()) return 0;
        return (int) Math.min(Integer.MAX_VALUE, (long) stored.getAmount() + free());
    }
    @Override public boolean isFluidValid(int tank, FluidStack stack) {
        if (tank < 0 || tank >= getTanks() || stack.isEmpty()) return false;
        FluidStack stored = getFluidInTank(tank);
        if (!stored.isEmpty()) return FluidStack.isSameFluidSameComponents(stored, stack);
        if (kinds() >= maxTypes.getAsInt()) return false;
        for (FluidStack fluid : contents) if (FluidStack.isSameFluidSameComponents(fluid, stack)) return false;
        return true;
    }
    @Override public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return 0;
        int accepted = Math.min(resource.getAmount(), free());
        if (accepted <= 0) return 0;
        int slot = -1, empty = -1;
        for (int i = 0; i < contents.size(); i++) {
            FluidStack fluid = contents.get(i);
            if (FluidStack.isSameFluidSameComponents(fluid, resource)) { slot = i; break; }
            if (fluid.isEmpty() && empty == -1) empty = i;
        }
        if (slot == -1) {
            if (kinds() >= maxTypes.getAsInt()) return 0;
            slot = empty == -1 ? contents.size() : empty;
        }
        if (action.execute()) {
            if (slot == contents.size()) contents.add(resource.copyWithAmount(accepted));
            else if (contents.get(slot).isEmpty()) contents.set(slot, resource.copyWithAmount(accepted));
            else contents.get(slot).grow(accepted);
            changed.run();
        }
        return accepted;
    }
    @Override public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return FluidStack.EMPTY;
        int taken = 0;
        for (int tank = 0; tank < contents.size(); tank++) {
            if (FluidStack.isSameFluidSameComponents(contents.get(tank), resource))
                taken += drainSlot(tank, resource.getAmount() - taken, action).getAmount();
            if (taken == resource.getAmount()) break;
        }
        return taken == 0 ? FluidStack.EMPTY : resource.copyWithAmount(taken);
    }
    @Override public FluidStack drain(int amount, FluidAction action) {
        if (amount <= 0) return FluidStack.EMPTY;
        for (int tank = 0; tank < contents.size(); tank++) if (!contents.get(tank).isEmpty()) return drainSlot(tank, amount, action);
        return FluidStack.EMPTY;
    }
    private FluidStack drainSlot(int tank, int amount, FluidAction action) {
        FluidStack stored = contents.get(tank);
        int taken = Math.min(amount, stored.getAmount());
        FluidStack result = stored.copyWithAmount(taken);
        if (action.execute()) {
            stored.shrink(taken);
            if (stored.isEmpty()) contents.set(tank, FluidStack.EMPTY);
            changed.run();
        }
        return result;
    }
    public ListTag save(HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (FluidStack fluid : contents) list.add(fluid.saveOptional(registries));
        return list;
    }
    public void load(ListTag list, HolderLookup.Provider registries) {
        contents.clear();
        int room = capacity();
        boolean clipped = false;
        for (int i = 0; i < list.size(); i++) {
            FluidStack fluid = FluidStack.parseOptional(registries, list.getCompound(i));
            int kept = Math.min(room, fluid.getAmount());
            clipped |= kept < fluid.getAmount();
            contents.add(kept == 0 ? FluidStack.EMPTY : fluid.copyWithAmount(kept));
            room -= kept;
        }
        if (clipped) changed.run();
    }
    public List<FluidStack> takeAll() {
        List<FluidStack> result = contents.stream().filter(fluid -> !fluid.isEmpty()).map(FluidStack::copy).toList();
        contents.clear();
        if (!result.isEmpty()) changed.run();
        return result;
    }
    /** Formation ownership transfer only; final structure capacity is not available during member assignment. */
    void absorbOwned(List<FluidStack> incoming) {
        for (var fluid : incoming) {
            int left = fluid.getAmount();
            for (var stored : contents) if (FluidStack.isSameFluidSameComponents(stored, fluid)) {
                int moved = (int) Math.min(left, (long) Integer.MAX_VALUE - stored.getAmount());
                stored.grow(moved);
                left -= moved;
                if (left == 0) break;
            }
            if (left > 0) {
                int empty = -1;
                for (int i = 0; i < contents.size(); i++) if (contents.get(i).isEmpty()) { empty = i; break; }
                if (empty == -1) contents.add(fluid.copyWithAmount(left));
                else contents.set(empty, fluid.copyWithAmount(left));
            }
        }
        if (!incoming.isEmpty()) changed.run();
    }
}
