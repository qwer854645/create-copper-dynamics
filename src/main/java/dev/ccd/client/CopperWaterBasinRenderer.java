package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Keep native animated water rendering, scaled vertically to the shallow kitchen bowl. */
public final class CopperWaterBasinRenderer extends FluidTankRenderer {
    public CopperWaterBasinRenderer(BlockEntityRendererProvider.Context context) { super(context); }

    @Override protected void renderSafe(FluidTankBlockEntity entity, float partialTicks, PoseStack pose,
        MultiBufferSource buffer, int light, int overlay) {
        pose.pushPose();
        pose.scale(1, .5f, 1);
        super.renderSafe(entity, partialTicks, pose, buffer, light, overlay);
        pose.popPose();
    }
}
