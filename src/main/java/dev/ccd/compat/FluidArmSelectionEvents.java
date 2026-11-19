package dev.ccd.compat;

import dev.ccd.CopperDynamics;
import dev.ccd.content.FluidArmItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid=CopperDynamics.ID)
public final class FluidArmSelectionEvents {
    @SubscribeEvent public static void deselect(PlayerInteractEvent.LeftClickBlock event) {
        var player=event.getEntity();
        if(player.isSpectator()) return;
        var held=player.getMainHandItem();
        if(held.getItem() instanceof FluidArmItem
            && FluidArmItem.deselect(held,event.getLevel(),event.getPos(),player)) event.setCanceled(true);
    }
}
