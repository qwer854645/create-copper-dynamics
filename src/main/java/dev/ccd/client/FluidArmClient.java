package dev.ccd.client;

import dev.ccd.CopperDynamics;
import dev.ccd.content.FluidArmItem;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = CopperDynamics.ID, value = Dist.CLIENT)
public final class FluidArmClient {
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        var held = minecraft.player.getMainHandItem();
        if (!(held.getItem() instanceof FluidArmItem)) return;
        for (var selected : FluidArmItem.selected(held, minecraft.level)) {
            if (!minecraft.level.hasChunkAt(selected.pos())) continue;
            Vec3 center = Vec3.atCenterOf(selected.pos()).add(Vec3.atLowerCornerOf(selected.face().getNormal()).scale(.51));
            double x = selected.face().getStepX() == 0 ? .35 : .025;
            double y = selected.face().getStepY() == 0 ? .35 : .025;
            double z = selected.face().getStepZ() == 0 ? .35 : .025;
            Outliner.getInstance().showAABB(selected, new AABB(center.x - x, center.y - y, center.z - z,
                center.x + x, center.y + y, center.z + z)).colored(selected.input() ? 0x7FCDE0 : 0xDDC166).lineWidth(1 / 32f);
        }
    }
}
