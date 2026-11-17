package dev.ccd.content;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** BasinRecipe already includes native crafting remainders in its capacity check and output transaction. */
public final class HoneycombMoldItem extends Item {
    public HoneycombMoldItem(Properties properties) { super(properties); }
    @Override public boolean hasCraftingRemainingItem(ItemStack stack) { return true; }
    @Override public ItemStack getCraftingRemainingItem(ItemStack stack) { return stack.copyWithCount(1); }
}
