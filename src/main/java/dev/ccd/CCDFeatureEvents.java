package dev.ccd;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = CopperDynamics.ID)
public final class CCDFeatureEvents {
    private CCDFeatureEvents() {}
    public static void creativeContents(BuildCreativeModeTabContentsEvent event) {
        for (var stack : java.util.List.copyOf(event.getParentEntries()))
            if (!CCDFeatures.enabled(stack.getItem())) event.remove(stack, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
        for (var stack : java.util.List.copyOf(event.getSearchEntries()))
            if (!CCDFeatures.enabled(stack.getItem())) event.remove(stack, net.minecraft.world.item.CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
    }
    @SubscribeEvent public static void useBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!CCDFeatures.enabled(event.getItemStack().getItem()) || !CCDFeatures.enabled(event.getLevel().getBlockState(event.getPos()))) {
            // Wrench pickup remains available for disabled devices and their saved contents.
            if (event.getItemStack().is(net.neoforged.neoforge.common.Tags.Items.TOOLS_WRENCH)) return;
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        }
    }
    @SubscribeEvent public static void useItem(PlayerInteractEvent.RightClickItem event) {
        if (!CCDFeatures.enabled(event.getItemStack().getItem())) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        }
    }
}
