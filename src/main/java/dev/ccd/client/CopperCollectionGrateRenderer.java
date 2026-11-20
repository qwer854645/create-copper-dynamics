package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.ccd.content.CopperCollectionGrateBlockEntity;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class CopperCollectionGrateRenderer extends SmartBlockEntityRenderer<CopperCollectionGrateBlockEntity> {
    public CopperCollectionGrateRenderer(BlockEntityRendererProvider.Context context) { super(context); }
    @Override protected void renderSafe(CopperCollectionGrateBlockEntity grate, float partialTicks, PoseStack pose,
        MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(grate, partialTicks, pose, buffer, light, overlay);
        var segment = grate.tankBehaviour().getPrimaryTank();
        if (segment.isEmpty(partialTicks)) return;
        // Keep the surface below the recessed bars, inside the ten-pixel-wide bowl.
        boolean restoredBowl=grate instanceof dev.ccd.content.SmartCollectionGrateBlockEntity;
        float bottom=(restoredBowl?3.0625f:8.0625f)/16;
        float height = Math.clamp(segment.getFluidLevel().getValue(partialTicks), 0f, 1f) * ((restoredBowl?10.75f:5.75f) / 16);
        NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(segment.getRenderedFluid(),
            3.0625f / 16, bottom, 3.0625f / 16,
            12.9375f / 16, bottom + height, 12.9375f / 16, buffer, pose, light, false, true);
    }
}
