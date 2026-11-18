package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ccd.content.CopperWaterSupplyBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ConduitRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;

/** Native animated conduit, contained inside the copper frame; never ticks a real conduit. */
public final class ConduitBoxRenderer implements BlockEntityRenderer<CopperWaterSupplyBlockEntity> {
    private final ConduitRenderer conduit;
    private final DisplayConduit display = new DisplayConduit();

    public ConduitBoxRenderer(BlockEntityRendererProvider.Context context) {
        conduit = new ConduitRenderer(context);
    }

    @Override public void render(CopperWaterSupplyBlockEntity entity, float partialTicks, PoseStack pose,
        MultiBufferSource buffer, int light, int overlay) {
        if (entity.getLevel() == null) return;
        // This client-only presentation object has no level, effects, target searches or world ticks.
        display.tickCount = (int) (entity.getLevel().getGameTime() % 24000L);
        pose.pushPose();
        pose.translate(.5, .5, .5);
        pose.scale(.55f, .55f, .55f);
        pose.translate(-.5, -.5, -.5);
        conduit.render(display, partialTicks, pose, buffer, light, overlay);
        pose.popPose();
    }

    private static final class DisplayConduit extends ConduitBlockEntity {
        private DisplayConduit() { super(BlockPos.ZERO, Blocks.CONDUIT.defaultBlockState()); }
        @Override public boolean isActive() { return true; }
        @Override public boolean isHunting() { return true; }
        @Override public float getActiveRotation(float partialTicks) { return (tickCount + partialTicks) * -.0375f; }
    }
}
