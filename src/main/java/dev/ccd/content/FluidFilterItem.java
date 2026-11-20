package dev.ccd.content;

import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import dev.ccd.CopperDynamics;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

/** A real Create filter item, with fluid-native samples instead of physical bucket entries. */
public final class FluidFilterItem extends FilterItem {
    public final boolean attributes;
    public FluidFilterItem(Properties properties, boolean attributes) {
        super(properties.stacksTo(1).component(CopperDynamics.FLUID_FILTER_DATA.get(), CustomData.EMPTY));
        this.attributes = attributes;
    }
    public static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(CopperDynamics.FLUID_FILTER_DATA.get(), CustomData.EMPTY).copyTag();
    }
    @Override public DataComponentType<?> getComponentType() { return CopperDynamics.FLUID_FILTER_DATA.get(); }
    @Override public ItemStack[] getFilterItems(ItemStack stack) { return new ItemStack[0]; }
    @Override public List<Component> makeSummary(ItemStack stack) {
        return List.of(Component.translatable(attributes ? "ccd.filter.attributes" : "ccd.filter.fluids"));
    }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FluidFilterMenu(id, inventory, player.getMainHandItem());
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || player.isShiftKeyDown()) return InteractionResultHolder.pass(player.getItemInHand(hand));
        if (!level.isClientSide()) player.openMenu(this, buffer -> ItemStack.STREAM_CODEC.encode(buffer, player.getMainHandItem()));
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
    @Override public FilterItemStack makeStackWrapper(ItemStack stack) {
        var settings = data(stack);
        return new FilterItemStack(stack) {
            @Override public boolean test(Level level, FluidStack fluid, boolean matchComponents) {
                return FluidFilterRules.matches(settings, attributes, level, fluid);
            }
            @Override public boolean test(Level level, ItemStack item, boolean matchComponents) {
                return test(level, FluidStockItem.sample(item), matchComponents);
            }
        };
    }
}
