package dev.ccd.client;

import dev.ccd.CopperDynamics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Release only CCD-generated variants after the world and its package instances leave. */
@EventBusSubscriber(modid = CopperDynamics.ID, value = Dist.CLIENT)
public final class PackageModelLifecycle {
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        FluidCanisterModel.clearWorldVariants();
        BrassBoxModel.clearWorldVariants();
    }
}
