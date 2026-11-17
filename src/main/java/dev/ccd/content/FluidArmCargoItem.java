package dev.ccd.content;

import dev.ccd.CopperDynamics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Internal held cargo: native arm persistence and rendering, without consuming packaging. */
public final class FluidArmCargoItem extends FluidPackageItem {
    public FluidArmCargoItem(Properties properties) { super(properties); }

    @Override public String getDescriptionId() { return "item.ccd.fluid_arm_cargo"; }

    public static ItemStack containing(FluidStack fluid) {
        if (fluid.isEmpty()) return ItemStack.EMPTY;
        ItemStack cargo = new ItemStack(CopperDynamics.FLUID_ARM_CARGO.get());
        cargo.set(CopperDynamics.FLUID_CARGO.get(), new FluidCargo(fluid));
        return cargo;
    }

    /** Also upgrades previously saved ordinary fluid packages held by this arm. */
    public static ItemStack inClaw(ItemStack stack) {
        if (!FluidPackageItem.isValid(stack) || stack.getItem() instanceof FluidArmCargoItem) return stack;
        return copyAs(stack, CopperDynamics.FLUID_ARM_CARGO.get());
    }

    /** Only materialize an ordinary sealed package when unloading or destroying the arm. */
    public static ItemStack sealed(ItemStack stack) {
        return stack.getItem() instanceof FluidArmCargoItem
            ? copyAs(stack, CopperDynamics.FLUID_PACKAGE.get()) : stack;
    }

    private static ItemStack copyAs(ItemStack stack, Item item) {
        ItemStack copy = new ItemStack(item, stack.getCount());
        copy.applyComponents(stack.getComponentsPatch());
        return copy;
    }
}
