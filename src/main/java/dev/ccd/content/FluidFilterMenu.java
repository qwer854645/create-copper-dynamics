package dev.ccd.content;

import dev.ccd.CopperDynamics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Settings-only menu: samples never enter an inventory and never consume cargo. */
public final class FluidFilterMenu extends AbstractContainerMenu {
    public final ItemStack filter;
    public final boolean attributes;
    public final int selectedSlot;
    public FluidFilterMenu(int id, Inventory inventory, ItemStack filter) {
        super(CopperDynamics.FLUID_FILTER_MENU.get(), id);
        this.filter = filter;
        this.attributes = filter.getItem() instanceof FluidFilterItem item && item.attributes;
        this.selectedSlot = inventory.selected;
    }
    @Override public boolean stillValid(Player player) {
        return player.getInventory().selected == selectedSlot && player.getMainHandItem() == filter;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
