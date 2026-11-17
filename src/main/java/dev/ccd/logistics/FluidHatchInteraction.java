package dev.ccd.logistics;

import dev.ccd.CopperDynamics;
import dev.ccd.content.FluidHatchBlock;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Native ItemHatchHandler checks its exact block, so the fluid subclass needs its own scoped hook. */
@EventBusSubscriber(modid = CopperDynamics.ID)
public final class FluidHatchInteraction {
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void useContainerWhileSneaking(PlayerInteractEvent.RightClickBlock event) {
        if (event.getUseItem() == TriState.DEFAULT
            && event.getUseBlock() != TriState.FALSE
            && event.getLevel().getBlockState(event.getPos()).getBlock() instanceof FluidHatchBlock
            && !event.getItemStack().is(Tags.Items.TOOLS_WRENCH)
            && FluidHatchBlock.isFluidContainer(event.getLevel(), event.getItemStack())) {
            event.setUseBlock(TriState.TRUE);
        }
    }
}
