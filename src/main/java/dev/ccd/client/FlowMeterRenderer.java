package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.ccd.content.FlowMeterBlock;
import dev.ccd.content.FlowMeterBlockEntity;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

public final class FlowMeterRenderer extends SafeBlockEntityRenderer<FlowMeterBlockEntity> {
    public FlowMeterRenderer(BlockEntityRendererProvider.Context context){}
    @Override protected void renderSafe(FlowMeterBlockEntity be,float pt,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var state=be.getBlockState();var face=state.getValue(FlowMeterBlock.FACING);
        float rotation=(float)((-face.toYRot()-90)/180*Math.PI);
        float progress=net.minecraft.util.Mth.lerp(pt,be.previousDial,be.dial),pivot=5.75f/16;
        CachedBuffers.partial(AllPartialModels.GAUGE_DIAL,state).rotateCentered(rotation,Direction.UP)
            .translate(0,pivot,pivot).rotate((float)(-Math.PI/2*progress),Direction.EAST).translate(0,-pivot,-pivot)
            .light(light).overlay(overlay).renderInto(pose,buffers.getBuffer(RenderType.cutoutMipped()));
    }
}
