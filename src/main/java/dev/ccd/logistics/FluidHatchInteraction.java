package dev.ccd.logistics;

import dev.ccd.CopperDynamics;
import dev.ccd.content.FluidHatchBlock;
import dev.ccd.content.CopperWaterBasinBlock;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Reuse native ItemHatchHandler's sneak-bypass mechanism for CCD's fluid interaction blocks. */
@EventBusSubscriber(modid = CopperDynamics.ID)
public final class FluidHatchInteraction {
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void useContainerWhileSneaking(PlayerInteractEvent.RightClickBlock event) {
        var block = event.getLevel().getBlockState(event.getPos()).getBlock();
        if (event.getUseItem() == TriState.DEFAULT
            && event.getUseBlock() != TriState.FALSE
            && (block instanceof FluidHatchBlock || block instanceof CopperWaterBasinBlock)
            && !event.getItemStack().is(Tags.Items.TOOLS_WRENCH)
            && FluidHatchBlock.isFluidContainer(event.getLevel(), event.getItemStack())) {
            event.setUseBlock(TriState.TRUE);
        }
    }
}
