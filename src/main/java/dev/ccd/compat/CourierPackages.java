package dev.ccd.compat;

import java.util.ArrayList;
import java.util.List;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import dev.ccd.content.CompositePackageItem;
import dev.ccd.content.FluidPackageItem;
import dev.ccd.content.FluidStockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Read-only presentation adapters. These tokens must never be used as physical cargo. */
public final class CourierPackages {
    private CourierPackages() {}
    public static boolean hasFluid(ItemStack box) {
        return !FluidPackageItem.contents(box).isEmpty() || !CompositePackageItem.fluids(box).isEmpty();
    }
    public static List<BigItemStack> displayEntries(ItemStack box) {
        var entries = new ArrayList<BigItemStack>();
        var items = PackageItem.getContents(box);
        for (int i=0;i<items.getSlots();i++) {
            var item=items.getStackInSlot(i);
            if (!item.isEmpty()) entries.add(new BigItemStack(item.copy(),item.getCount()));
        }
        var fluid=FluidPackageItem.contents(box);
        if (!fluid.isEmpty()) entries.add(new BigItemStack(FluidStockItem.of(fluid),fluid.getAmount()));
        for (var content:CompositePackageItem.fluids(box))
            entries.add(new BigItemStack(FluidStockItem.of(content),content.getAmount()));
        return entries;
    }
    public static ItemStackHandler toastContents(ItemStack box) {
        if (!hasFluid(box)) return PackageItem.getContents(box);
        var entries=displayEntries(box);
        var display=new ItemStackHandler(entries.size());
        for(int i=0;i<entries.size();i++) {
            var entry=entries.get(i);
            var icon=entry.stack.copy();
            var fluid=FluidStockItem.fluid(icon);
            if(!fluid.isEmpty()) icon.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.empty().append(fluid.getHoverName()).append(" — ")
                    .append(dev.ccd.CCDTooltip.amount(entry.count)));
            display.setStackInSlot(i,icon);
        }
        return display;
    }
}
