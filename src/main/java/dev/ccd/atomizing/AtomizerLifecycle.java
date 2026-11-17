package dev.ccd.atomizing;

import dev.ccd.CopperDynamics;
import dev.ccd.api.AtomizerScripts;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@EventBusSubscriber(modid = CopperDynamics.ID)
public final class AtomizerLifecycle {
    @SubscribeEvent public static void reload(AddReloadListenerEvent event) {
        event.addListener((ResourceManagerReloadListener) resources -> AtomizingRules.invalidate());
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { AtomizerScripts.clear(); }
}
