package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.ccd.content.CopperCollectionGrateBlockEntity;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class CopperCollectionGrateRenderer extends SafeBlockEntityRenderer<CopperCollectionGrateBlockEntity> {
    public CopperCollectionGrateRenderer(BlockEntityRendererProvider.Context context) {}
    @Override protected void renderSafe(CopperCollectionGrateBlockEntity grate, float partialTicks, PoseStack pose,
        MultiBufferSource buffer, int light, int overlay) {
        var segment = grate.tankBehaviour().getPrimaryTank();
        if (segment.isEmpty(partialTicks)) return;
        float height = Math.clamp(segment.getFluidLevel().getValue(partialTicks), 0f, 1f) * .11f;
        NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(segment.getRenderedFluid(),
            .07f, .065f, .07f, .93f, .065f + height, .93f, buffer, pose, light, false, true);
    }
}
