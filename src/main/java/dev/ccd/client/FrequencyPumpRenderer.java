package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.ccd.content.VariableFlowPumpBlock;
import dev.ccd.content.VariableFlowPumpBlockEntity;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public final class FrequencyPumpRenderer extends SafeBlockEntityRenderer<VariableFlowPumpBlockEntity> {
    private static final PartialModel COG=PartialModel.of(ResourceLocation.fromNamespaceAndPath("ccd","block/variable_flow_pump/cog"));
    public static void registerModels(){}
    public FrequencyPumpRenderer(BlockEntityRendererProvider.Context context){}
    @Override protected void renderSafe(VariableFlowPumpBlockEntity be,float pt,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var state=be.getBlockState();var front=state.getValue(VariableFlowPumpBlock.FACING);
        float time=net.createmod.catnip.animation.AnimationTickHolder.getRenderTime(be.getLevel());
        var axis=((VariableFlowPumpBlock)state.getBlock()).getRotationAxis(state);
        float offset=KineticBlockEntityRenderer.getRotationOffsetForPosition(be,be.getBlockPos(),axis);
        float angle=(float)Math.toRadians((time*be.getSpeed()*.3f+offset)%360);
        pose.pushPose();pose.translate(.5,.5,.5);
        PumpRenderFrame.apply(pose,state,VariableFlowPumpBlock.ROLL);
        pose.mulPose(Axis.XP.rotation(angle));
        pose.translate(-.5,-.5,-.5);
        CachedBuffers.partial(COG,state).light(light).overlay(overlay).renderInto(pose,buffers.getBuffer(RenderType.cutoutMipped()));pose.popPose();
        // The dial measures signal, not shaft speed, and remains live while the pump is stopped.
        float progress=net.minecraft.util.Mth.lerp(pt,be.previousSignalDial,be.signalDial);
        for(int side:new int[]{1,-1}){
            pose.pushPose();pose.translate(.5,.5,.5);
            PumpRenderFrame.apply(pose,state,VariableFlowPumpBlock.ROLL);
            if(side<0)pose.mulPose(Axis.YP.rotationDegrees(180));
            pose.translate(-.5,-.5,-.5);
            pose.translate(-1.9/16f,0,0);
            float pivot=5.75f/16;
            pose.translate(1,pivot,pivot);pose.mulPose(Axis.XP.rotationDegrees(-90*progress));pose.translate(-1,-pivot,-pivot);
            CachedBuffers.partial(AllPartialModels.GAUGE_DIAL,state).light(light).overlay(overlay)
                .renderInto(pose,buffers.getBuffer(RenderType.cutoutMipped()));
            pose.popPose();
        }
    }
}
