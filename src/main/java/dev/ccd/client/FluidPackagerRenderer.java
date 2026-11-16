package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.content.logistics.packager.PackagerRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.mojang.math.Axis;
import dev.ccd.content.FluidPackagerBlockEntity;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

/** Native moving parts through Create's renderer API; CCD needs no Flywheel visual registration. */
public final class FluidPackagerRenderer extends SmartBlockEntityRenderer<FluidPackagerBlockEntity> {
    public FluidPackagerRenderer(BlockEntityRendererProvider.Context context) { super(context); }
    @Override protected void renderSafe(FluidPackagerBlockEntity be, float partialTicks, PoseStack pose,
        MultiBufferSource buffers, int light, int overlay) {
        super.renderSafe(be, partialTicks, pose, buffers, light, overlay);
        var state = be.getBlockState();
        var rear = state.getValue(PackagerBlock.FACING).getOpposite();
        var direction = Vec3.atLowerCornerOf(rear.getNormal());
        CachedBuffers.partial(PackagerRenderer.getHatchModel(be), state)
            .translate(direction.scale(.49999))
            .rotateYCenteredDegrees(AngleHelper.horizontalAngle(rear))
            .rotateXCenteredDegrees(AngleHelper.verticalAngle(rear))
            .light(light).renderInto(pose, buffers.getBuffer(RenderType.solid()));
        float offset = be.getTrayOffset(partialTicks);
        CachedBuffers.partial(AllPartialModels.PACKAGER_TRAY_REGULAR, state)
            .translate(direction.scale(offset)).rotateYCenteredDegrees(rear.toYRot())
            .light(light).renderInto(pose, buffers.getBuffer(RenderType.cutoutMipped()));
        var box = be.getRenderedBox();
        if (box.isEmpty()) return;
        pose.pushPose();
        var shift = direction.scale(offset);
        pose.translate(.5 + shift.x, .625 + shift.y, .5 + shift.z);
        pose.mulPose(Axis.YP.rotationDegrees(rear.toYRot()));
        pose.scale(1.49F, 1.49F, 1.49F);
        Minecraft.getInstance().getItemRenderer().renderStatic(null, box, ItemDisplayContext.FIXED, false,
            pose, buffers, be.getLevel(), light, overlay, 0);
        pose.popPose();
    }
}
