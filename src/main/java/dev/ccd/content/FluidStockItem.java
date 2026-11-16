package dev.ccd.content;

import java.util.List;
import dev.ccd.CopperDynamics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

/** Identity adapter for Create's ItemStack-based logistics: one count means one mB.
 * Never inserted into physical inventories by CCD; actual cargo is a FluidPackageItem. */
public final class FluidStockItem extends Item {
    public FluidStockItem(Properties properties) { super(properties.stacksTo(1)); }
    public static ItemStack of(FluidStack fluid) {
        if (fluid.isEmpty()) return ItemStack.EMPTY;
        ItemStack token = new ItemStack(CopperDynamics.FLUID_STOCK.get());
        token.set(CopperDynamics.FLUID_CARGO.get(), new FluidCargo(fluid.copyWithAmount(1)));
        return token;
    }
    public static FluidStack fluid(ItemStack stack) {
        if (!(stack.getItem() instanceof FluidStockItem)) return FluidStack.EMPTY;
        FluidCargo cargo = stack.get(CopperDynamics.FLUID_CARGO.get());
        return cargo == null ? FluidStack.EMPTY : cargo.fluid();
    }
    public static FluidStack sample(ItemStack stack) {
        if (stack.isEmpty()) return FluidStack.EMPTY;
        FluidStack token = fluid(stack);
        if (!token.isEmpty()) return token;
        FluidStack cargo = FluidPackageItem.contents(stack);
        if (!cargo.isEmpty()) return cargo.copyWithAmount(1);
        var handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler != null) for (int i = 0; i < handler.getTanks(); i++) {
            FluidStack content = handler.getFluidInTank(i);
            if (!content.isEmpty()) return content.copyWithAmount(1);
        }
        return FluidStack.EMPTY;
    }
    @Override public Component getName(ItemStack stack) {
        FluidStack fluid = fluid(stack);
        return fluid.isEmpty() ? Component.translatable("item.ccd.fluid_stock")
            : Component.translatable("ccd.fluid_stock.name", fluid.getHoverName());
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("ccd.fluid_stock.explanation"));
    }
}
