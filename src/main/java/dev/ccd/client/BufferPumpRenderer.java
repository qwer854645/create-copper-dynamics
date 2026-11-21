package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.ccd.content.BufferPumpBlock;
import dev.ccd.content.BufferPumpBlockEntity;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

public final class BufferPumpRenderer extends SafeBlockEntityRenderer<BufferPumpBlockEntity> {
    private static final dev.engine_room.flywheel.lib.model.baked.PartialModel COG=dev.engine_room.flywheel.lib.model.baked.PartialModel.of(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ccd","block/buffer_pump/cog"));
    public static void registerModels(){}
    public BufferPumpRenderer(BlockEntityRendererProvider.Context context){}
    @Override protected void renderSafe(BufferPumpBlockEntity be,float pt,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var state=be.getBlockState();var axis=((BufferPumpBlock)state.getBlock()).getRotationAxis(state);
        float time=net.createmod.catnip.animation.AnimationTickHolder.getRenderTime(be.getLevel());
        float angle=(float)Math.toRadians((time*be.getSpeed()*.3f)%360);
        pose.pushPose();pose.translate(.5,.5,.5);
        var facing=state.getValue(BufferPumpBlock.FACING);
        switch(facing){
            case DOWN->pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180));
            case NORTH->pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));
            case SOUTH->pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
            case EAST->{pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90));pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));}
            case WEST->{pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));}
            default->{}
        }
        pose.translate(-.5,-.5,-.5);
        pose.pushPose();pose.translate(.5,10/16d,.5);
        pose.mulPose(com.mojang.math.Axis.XP.rotation(angle));pose.translate(-.5,-10/16d,-.5);
        CachedBuffers.partial(COG,state).light(light).renderInto(pose,buffers.getBuffer(RenderType.solid()));pose.popPose();
        if(!be.buffer.isEmpty())net.createmod.catnip.platform.NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(be.buffer.getFluid(),
            3/16f,1/16f,3/16f,13/16f,(1+3.5f*be.buffer.getFluidAmount()/4000)/16,13/16f,buffers,pose,light,false,true);
        pose.popPose();
        for(var side:Direction.values())if(side.getAxis()==axis)
            KineticBlockEntityRenderer.standardKineticRotationTransform(CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF,state,side),be,light).renderInto(pose,buffers.getBuffer(RenderType.solid()));
    }
}
