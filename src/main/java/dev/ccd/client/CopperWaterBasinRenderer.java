package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Keep native animated water rendering, positioned in the inset metal kitchen bowl. */
public final class CopperWaterBasinRenderer extends FluidTankRenderer {
    public CopperWaterBasinRenderer(BlockEntityRendererProvider.Context context) { super(context); }

    @Override protected void renderSafe(FluidTankBlockEntity entity, float partialTicks, PoseStack pose,
        MultiBufferSource buffer, int light, int overlay) {
        pose.pushPose();
        // Native full water spans Y=5..12 pixels; map it to Y=9.625..14 inside the inset bowl.
        pose.translate(0, 6.5 / 16d, 0);
        pose.scale(1, .625f, 1);
        super.renderSafe(entity, partialTicks, pose, buffer, light, overlay);
        pose.popPose();
    }
}
