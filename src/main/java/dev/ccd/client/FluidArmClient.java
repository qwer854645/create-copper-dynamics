package dev.ccd.client;

import dev.ccd.CopperDynamics;
import dev.ccd.content.FluidArmItem;
import dev.ccd.logistics.FluidArmPoint;
import dev.ccd.content.FluidArmBlockEntity;
import dev.ccd.mixin.FluidArmAccessor;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint.Mode;
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
        java.util.List<FluidArmItem.Selected> points;
        boolean selecting=held.getItem() instanceof FluidArmItem;
        if(selecting) points=FluidArmItem.selected(held,minecraft.level);
        else if(held.is(net.neoforged.neoforge.common.Tags.Items.TOOLS_WRENCH)
            && minecraft.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit
            && minecraft.level.getBlockEntity(hit.getBlockPos()) instanceof FluidArmBlockEntity arm) {
            var access=(FluidArmAccessor)(Object)arm;
            points=new java.util.ArrayList<>();
            for(var point:access.ccd$inputs()) if(point instanceof FluidArmPoint fluid)
                points.add(new FluidArmItem.Selected(point.getPos(),fluid.face(),true));
            for(var point:access.ccd$outputs()) if(point instanceof FluidArmPoint fluid)
                points.add(new FluidArmItem.Selected(point.getPos(),fluid.face(),false));
        } else return;
        for (var selected : points) {
            if (!FluidArmPoint.canTarget(minecraft.level, selected.pos())
                || FluidArmPoint.interactionHandler(minecraft.level,selected.pos(),selected.face())==null) continue;
            int color=(selected.input()?Mode.TAKE:Mode.DEPOSIT).getColor();
            var shape=minecraft.level.getBlockState(selected.pos()).getShape(minecraft.level,selected.pos());
            if(!shape.isEmpty()) Outliner.getInstance().showAABB(java.util.List.of("ccd_arm_target",selected),
                shape.bounds().move(selected.pos())).colored(color).lineWidth(1/16f);
            Vec3 center = Vec3.atCenterOf(selected.pos()).add(Vec3.atLowerCornerOf(selected.face().getNormal()).scale(.51));
            double x = selected.face().getStepX() == 0 ? .35 : .025;
            double y = selected.face().getStepY() == 0 ? .35 : .025;
            double z = selected.face().getStepZ() == 0 ? .35 : .025;
            Outliner.getInstance().showAABB(selected, new AABB(center.x - x, center.y - y, center.z - z,
                center.x + x, center.y + y, center.z + z)).colored(color).lineWidth(1 / 32f);
        }
    }
}
