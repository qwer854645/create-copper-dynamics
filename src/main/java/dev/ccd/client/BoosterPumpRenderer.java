package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.ccd.content.BoosterPumpBlock;
import dev.ccd.content.BoosterPumpBlockEntity;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/** Use native half-shafts without the default renderer's Flywheel early exit:
 * CCD has no separate visual, so these parts must also render with Flywheel enabled. */
public final class BoosterPumpRenderer extends SafeBlockEntityRenderer<BoosterPumpBlockEntity> {
    public BoosterPumpRenderer(BlockEntityRendererProvider.Context context) {}

    @Override protected void renderSafe(BoosterPumpBlockEntity be, float partialTicks, PoseStack pose,
        MultiBufferSource buffers, int light, int overlay) {
        var state = be.getBlockState();
        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        for (Direction face : Direction.values()) {
            float factor = BoosterPumpBlock.shaftModifier(state, face);
            if (factor == 0) continue;
            float offset = KineticBlockEntityRenderer.getRotationOffsetForPosition(be, be.getBlockPos(), face.getAxis());
            float angle = (float) Math.toRadians((time * be.getSpeed() * factor * .3f + offset) % 360);
            var shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, face);
            KineticBlockEntityRenderer.kineticRotationTransform(shaft, be, face.getAxis(), angle, light)
                .renderInto(pose, buffers.getBuffer(RenderType.solid()));
        }
    }
}
