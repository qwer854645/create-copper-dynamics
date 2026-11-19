package dev.ccd.gametest;

import java.util.List;
import dev.ccd.compat.CourierPackages;
import dev.ccd.content.CompositePackageItem;
import dev.ccd.content.FluidPackageItem;
import dev.ccd.content.FluidStockItem;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd")
@PrefixGameTestTemplate(false)
public final class CourierPackageTests {
    @GameTest(template="empty")
    public static void portableAmountsRespectUnitsAndBounds(GameTestHelper h) {
        h.assertTrue(dev.ccd.compat.CourierAmounts.adjust(0,true,false,false,0,5000,1)==1000,"Bucket increment incorrect");
        h.assertTrue(dev.ccd.compat.CourierAmounts.adjust(1000,true,true,false,0,5000,1)==1100,"Shift increment incorrect");
        h.assertTrue(dev.ccd.compat.CourierAmounts.adjust(1000,false,false,true,0,5000,1)==999,"Precise decrement incorrect");
        h.assertTrue(dev.ccd.compat.CourierAmounts.adjust(1000,true,false,false,0,1500,3)==1500,"Available stock exceeded");
        h.assertTrue(dev.ccd.compat.CourierAmounts.adjust(100,false,false,false,0,5000,1)==0,"Negative order amount");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void previewKeepsAmountsAndDoesNotChangeCargo(GameTestHelper h) {
        var items=new ItemStackHandler(9);
        items.setStackInSlot(0,new ItemStack(Items.DIAMOND,3));
        var box=CompositePackageItem.containing(items,List.of(new FluidStack(Fluids.WATER,2500),new FluidStack(Fluids.LAVA,1000)));
        var before=box.copy();
        var entries=CourierPackages.displayEntries(box);
        h.assertTrue(entries.size()==3,"Missing mixed cargo preview");
        h.assertTrue(entries.get(1).count==2500 && entries.get(2).count==1000,"Preview changed mB amounts");
        h.assertTrue(!FluidStockItem.fluid(entries.get(1).stack).isEmpty(),"Preview did not use a fluid identity token");
        h.assertTrue(CourierPackages.toastContents(box).getSlots()==3,"Toast omitted fluids");
        h.assertTrue(ItemStack.matches(before,box),"Presentation mutated physical cargo");
        h.assertTrue(CourierPackages.hasFluid(FluidPackageItem.containingFluid(new FluidStack(Fluids.WATER,3500))),"Copper cargo unrecognized");
        h.succeed();
    }
}
