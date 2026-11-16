package dev.ccd.logistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.repackager.PackageRepackageHelper;
import com.simibubi.create.content.logistics.packager.repackager.RepackagerBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import dev.ccd.content.CompositePackageItem;
import dev.ccd.content.FluidPackageItem;
import dev.ccd.content.FluidStockItem;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/** Native completeness checking and inventory scanning stay in Create. This helper
 * only replaces content repacking for complete orders containing CCD fluid cargo. */
public final class CompositeRepackageHelper extends PackageRepackageHelper {
    @Nullable private final RepackagerBlockEntity owner;
    private final Map<Integer, List<BigItemStack>> prepared = new HashMap<>();
    private final Set<Integer> blocked = new HashSet<>();
    public CompositeRepackageHelper(@Nullable RepackagerBlockEntity owner) { this.owner = owner; }
    @Override public void clear() { super.clear(); prepared.clear(); blocked.clear(); }

    @Override public int addPackageFragment(ItemStack box) {
        int order = PackageItem.getOrderId(box);
        if (order == -1 || blocked.contains(order)) return -1;
        int completed = super.addPackageFragment(box);
        List<ItemStack> fragments = collectedPackages.get(order);
        boolean hasFluids = fragments != null && fragments.stream().anyMatch(CompositePackageItem::isSealed);
        if (!hasFluids) {
            if (completed != -1 && owner != null && hasUnscannedFluid(order)) { blocked.add(order); return -1; }
            return completed;
        }
        if (!readable(fragments)) { blocked.add(order); return -1; }
        if (completed == -1) return -1;
        // The native machine removes all matching-order input parcels after repack().
        // Refuse extra or duplicate parcels which its early completion scan did not see.
        if (owner != null && !inventoryMatches(order, fragments.size())) { blocked.add(order); return -1; }
        List<BigItemStack> result = prepare(order, fragments);
        if (result.isEmpty()) { blocked.add(order); return -1; }
        if (owner != null) {
            owner.updateSignAddress();
            if (!owner.signBasedAddress.isBlank())
                for (BigItemStack parcel : result) PackageItem.addAddress(parcel.stack, owner.signBasedAddress);
        }
        prepared.put(order, result);
        return completed;
    }

    private boolean inventoryMatches(int order, int expected) {
        var inventory = owner.targetInventory.getInventory();
        if (inventory == null) return false;
        int count = 0;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack parcel = inventory.extractItem(i, 1, true);
            if (PackageItem.isPackage(parcel) && PackageItem.getOrderId(parcel) == order) count++;
        }
        return count == expected;
    }
    private boolean hasUnscannedFluid(int order) {
        var inventory = owner.targetInventory.getInventory();
        if (inventory == null) return false;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack parcel = inventory.extractItem(i, 1, true);
            if (CompositePackageItem.isSealed(parcel) && PackageItem.getOrderId(parcel) == order) return true;
        }
        return false;
    }

    private static boolean readable(List<ItemStack> fragments) {
        String address = null;
        Set<Long> indices = new HashSet<>();
        for (ItemStack parcel : fragments) {
            if (FluidPackageItem.isFluidPackage(parcel)) {
                if (!FluidPackageItem.isValid(parcel)) return false;
            } else if (CompositePackageItem.isComposite(parcel)) {
                if (!CompositePackageItem.isValid(parcel)) return false;
            } else if (parcel.getItem().getClass() != PackageItem.class) return false;
            String current = PackageItem.getAddress(parcel);
            if (address != null && !address.equals(current)) return false;
            address = current;
            var data = parcel.get(AllDataComponents.PACKAGE_ORDER_DATA);
            if (data == null || data.linkIndex() < 0 || data.fragmentIndex() < 0) return false;
            long key = ((long) data.linkIndex() << 32) | Integer.toUnsignedLong(data.fragmentIndex());
            if (!indices.add(key)) return false;
            var items = PackageItem.getContents(parcel);
            for (int i = 0; i < items.getSlots(); i++) {
                ItemStack item = items.getStackInSlot(i);
                if (PackageItem.isPackage(item) || item.getItem() instanceof FluidStockItem) return false;
            }
        }
        return true;
    }

    @Override public List<BigItemStack> repack(int order, RandomSource random) {
        List<BigItemStack> result = prepared.get(order);
        if (result != null) return result;
        // Ordinary orders retain Create's exact recipe-aware packing implementation.
        var fragments = collectedPackages.get(order);
        if (fragments != null && fragments.stream().anyMatch(CompositePackageItem::isSealed))
            throw new IllegalStateException("Fluid order was not prepared before consumption");
        return super.repack(order, random);
    }

    private static final class FluidAmount {
        final FluidStack prototype;
        long remaining;
        FluidAmount(FluidStack fluid) { prototype = fluid.copyWithAmount(1); remaining = fluid.getAmount(); }
    }
    private static void addFluid(List<FluidAmount> totals, FluidStack fluid) {
        for (FluidAmount total : totals) if (FluidStack.isSameFluidSameComponents(total.prototype, fluid)) {
            total.remaining += fluid.getAmount(); return;
        }
        totals.add(new FluidAmount(fluid));
    }

    private static List<BigItemStack> prepare(int order, List<ItemStack> fragments) {
        InventorySummary itemTotals = new InventorySummary();
        List<FluidAmount> fluidTotals = new ArrayList<>();
        PackageOrderWithCrafts context = null;
        for (ItemStack parcel : fragments) {
            var data = parcel.get(AllDataComponents.PACKAGE_ORDER_DATA);
            if (data.orderContext() != null && !data.orderContext().isEmpty()) context = data.orderContext();
            var items = PackageItem.getContents(parcel);
            for (int i = 0; i < items.getSlots(); i++) itemTotals.add(items.getStackInSlot(i));
            if (FluidPackageItem.isFluidPackage(parcel)) addFluid(fluidTotals, FluidPackageItem.contents(parcel));
            if (CompositePackageItem.isComposite(parcel)) for (FluidStack fluid : CompositePackageItem.fluids(parcel)) addFluid(fluidTotals, fluid);
        }
        List<BigItemStack> items = itemTotals.getStacks().stream().map(e -> new BigItemStack(e.stack.copy(), e.count)).toList();
        int itemIndex = 0, fluidIndex = 0;
        List<BigItemStack> result = new ArrayList<>();
        while (itemIndex < items.size() || fluidIndex < fluidTotals.size()) {
            // Native completeness checking supports fragment indices 0..999.
            if (result.size() == 1_000) return List.of();
            ItemStackHandler slots = new ItemStackHandler(PackageItem.SLOTS);
            for (int slot = 0; slot < slots.getSlots() && itemIndex < items.size(); slot++) {
                BigItemStack entry = items.get(itemIndex);
                int amount = Math.min(entry.count, entry.stack.getMaxStackSize());
                if (amount <= 0) return List.of();
                slots.setStackInSlot(slot, entry.stack.copyWithCount(amount));
                entry.count -= amount;
                if (entry.count == 0) itemIndex++;
            }
            List<FluidStack> fluids = new ArrayList<>();
            int space = CompositePackageItem.FLUID_CAPACITY;
            while (fluidIndex < fluidTotals.size() && space > 0 && fluids.size() < CompositePackageItem.MAX_FLUID_TYPES) {
                FluidAmount total = fluidTotals.get(fluidIndex);
                int amount = (int) Math.min(space, total.remaining);
                fluids.add(total.prototype.copyWithAmount(amount));
                total.remaining -= amount; space -= amount;
                if (total.remaining == 0) fluidIndex++;
            }
            result.add(new BigItemStack(CompositePackageItem.containing(slots, fluids), 1));
        }
        String address = PackageItem.getAddress(fragments.getFirst());
        for (int i = 0; i < result.size(); i++) {
            ItemStack parcel = result.get(i).stack;
            PackageItem.addAddress(parcel, address);
            boolean last = i == result.size() - 1;
            PackageItem.setOrder(parcel, order, 0, true, i, last, last ? context : null);
        }
        return result;
    }
}
