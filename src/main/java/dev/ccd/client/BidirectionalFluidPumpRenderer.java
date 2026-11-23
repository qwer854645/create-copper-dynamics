package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.ccd.content.BidirectionalFluidPumpBlock;
import dev.ccd.content.BidirectionalFluidPumpBlockEntity;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

public final class BidirectionalFluidPumpRenderer extends SafeBlockEntityRenderer<BidirectionalFluidPumpBlockEntity> {
    public BidirectionalFluidPumpRenderer(BlockEntityRendererProvider.Context context) {}
    @Override protected void renderSafe(BidirectionalFluidPumpBlockEntity be, float partialTicks, PoseStack pose,
        MultiBufferSource buffers, int light, int overlay) {
        var state = be.getBlockState();
        PumpContainerAttachmentRenderer.render(be,pose,buffers,light,overlay);
        var axis = ((BidirectionalFluidPumpBlock) state.getBlock()).getRotationAxis(state);
        for (Direction side : Direction.values()) if (side.getAxis() == axis) {
            var shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, side);
            KineticBlockEntityRenderer.standardKineticRotationTransform(shaft, be, light)
                .renderInto(pose, buffers.getBuffer(RenderType.solid()));
        }
    }
}
