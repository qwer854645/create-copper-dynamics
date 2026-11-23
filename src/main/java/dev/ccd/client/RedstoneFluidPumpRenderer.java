package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.ccd.content.RedstoneFluidPumpBlock;
import dev.ccd.content.RedstoneFluidPumpBlockEntity;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public final class RedstoneFluidPumpRenderer extends SafeBlockEntityRenderer<RedstoneFluidPumpBlockEntity> {
    private static final PartialModel POINTER=PartialModel.of(ResourceLocation.fromNamespaceAndPath("ccd","block/redstone_fluid_pump/pointer"));
    public static void registerModels() {}
    public RedstoneFluidPumpRenderer(BlockEntityRendererProvider.Context context) {}
    @Override protected void renderSafe(RedstoneFluidPumpBlockEntity be,float pt,PoseStack pose,MultiBufferSource buffer,int light,int overlay) {
        var state=be.getBlockState();var facing=state.getValue(RedstoneFluidPumpBlock.FACING);
        PumpContainerAttachmentRenderer.render(be,pose,buffer,light,overlay);
        var shaftAxis=((RedstoneFluidPumpBlock)state.getBlock()).getRotationAxis(state);
        for(var side:Direction.values()) if(side.getAxis()==shaftAxis) {
            var shaft=CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF,state,side);
            KineticBlockEntityRenderer.standardKineticRotationTransform(shaft,be,light)
                .renderInto(pose,buffer.getBuffer(RenderType.solid()));
        }
        pose.pushPose();pose.translate(.5,.5,.5);
        PumpRenderFrame.apply(pose,state,RedstoneFluidPumpBlock.ROLL);
        pose.mulPose(Axis.YP.rotationDegrees(180*be.pointer.getValue(pt)));
        pose.translate(-.5,-.5,-.5);
        CachedBuffers.partial(POINTER,state).light(light).overlay(overlay).renderInto(pose,buffer.getBuffer(RenderType.cutoutMipped()));
        pose.popPose();
    }
}
