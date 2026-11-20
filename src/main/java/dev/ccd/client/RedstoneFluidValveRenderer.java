package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlock;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.ccd.mixin.FluidValvePointerAccessor;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/** Render the native dial and pointer without the unused kinetic shaft of the mechanical valve. */
public final class RedstoneFluidValveRenderer extends SafeBlockEntityRenderer<FluidValveBlockEntity> {
    private static final PartialModel CLOSED = part("dial_closed"), OPEN = part("dial_open");
    private static PartialModel part(String name) {
        return PartialModel.of(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ccd", "block/redstone_fluid_valve/" + name));
    }
    public static void registerModels() {} // Retain the dial partials before model baking.
    public RedstoneFluidValveRenderer(BlockEntityRendererProvider.Context context) {}

    @Override protected void renderSafe(FluidValveBlockEntity be, float pt, PoseStack pose,
        MultiBufferSource buffers, int light, int overlay) {
        var state = be.getBlockState();
        Direction facing = state.getValue(FluidValveBlock.FACING);
        var pipeAxis = FluidValveBlock.getPipeAxis(state);
        var shaftAxis = KineticBlockEntityRenderer.getRotationAxisOf(be);
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
        // Keep pointers outside the restored outer dial, below its glass rim.
        pose.scale(1, 5.02f / 5.5f, 1);
        pose.translate(-.5, -.5, -.5);
        CachedBuffers.partial(AllPartialModels.FLUID_VALVE_POINTER, state)
            .light(light).overlay(overlay).renderInto(pose, buffers.getBuffer(RenderType.solid()));
        pose.popPose();
    }
}
