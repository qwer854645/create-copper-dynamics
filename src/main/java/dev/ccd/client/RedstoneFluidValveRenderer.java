package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlock;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlockEntity;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveRenderer;
import dev.ccd.mixin.FluidValvePointerAccessor;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/** Retain the native valve/visual, but draw its dial in front of the casing with exterior lighting. */
public final class RedstoneFluidValveRenderer extends FluidValveRenderer {
    private static final PartialModel CLOSED = part("dial_closed"), OPEN = part("dial_open");
    private static PartialModel part(String name) {
        return PartialModel.of(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ccd", "block/redstone_fluid_valve/" + name));
    }
    public static void registerModels() {} // Retain the dial partials before model baking.
    public RedstoneFluidValveRenderer(BlockEntityRendererProvider.Context context) { super(context); }

    @Override protected void renderSafe(FluidValveBlockEntity be, float pt, PoseStack pose,
        MultiBufferSource buffers, int light, int overlay) {
        super.renderSafe(be, pt, pose, buffers, light, overlay);
        var state = be.getBlockState();
        Direction facing = state.getValue(FluidValveBlock.FACING);
        var pipeAxis = FluidValveBlock.getPipeAxis(state);
        var shaftAxis = getRotationAxisOf(be);
        int offset = pipeAxis.isHorizontal() && shaftAxis == Direction.Axis.X || pipeAxis.isVertical() ? 90 : 0;
        float value = ((FluidValvePointerAccessor) (Object) be).ccd$getPointer().getValue(pt);
        pose.pushPose();
        pose.translate(.5, .5, .5);
        pose.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing)));
        pose.mulPose(Axis.XP.rotationDegrees(facing == Direction.UP ? 0 : facing == Direction.DOWN ? 180 : 90));
        pose.mulPose(Axis.YP.rotationDegrees(offset));
        pose.translate(-.5, -.5, -.5);
        CachedBuffers.partial(state.getValue(FluidValveBlock.ENABLED) ? OPEN : CLOSED, state)
            .light(light).overlay(overlay).renderInto(pose, buffers.getBuffer(RenderType.cutoutMipped()));
        pose.translate(.5, .5, .5);
        pose.mulPose(Axis.YP.rotationDegrees(-90 * value));
        // Native needles are at Y=2.5/13.5. Move only their depth beyond the outer dial at 1.99/14.01.
        pose.scale(1, 6.02f / 5.5f, 1);
        pose.translate(-.5, -.5, -.5);
        CachedBuffers.partial(AllPartialModels.FLUID_VALVE_POINTER, state)
            .light(light).overlay(overlay).renderInto(pose, buffers.getBuffer(RenderType.solid()));
        pose.popPose();
    }
}
