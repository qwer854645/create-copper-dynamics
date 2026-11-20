package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.ccd.content.ReciprocatingFluidPumpBlock;
import dev.ccd.content.ReciprocatingFluidPumpBlockEntity;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public final class PistonPumpRenderer extends SafeBlockEntityRenderer<ReciprocatingFluidPumpBlockEntity> {
    private static final PartialModel PISTON=part("piston"),CRANK=part("crank"),LINK=part("link");
    private static PartialModel part(String name){return PartialModel.of(ResourceLocation.fromNamespaceAndPath("ccd","block/piston_fluid_pump/"+name));}
    public static void registerModels(){}
    public PistonPumpRenderer(BlockEntityRendererProvider.Context context){}
    private static void draw(PartialModel part,ReciprocatingFluidPumpBlockEntity be,PoseStack pose,MultiBufferSource buffers,int light){
        CachedBuffers.partial(part,be.getBlockState()).light(light).renderInto(pose,buffers.getBuffer(RenderType.cutoutMipped()));
    }
    @Override protected void renderSafe(ReciprocatingFluidPumpBlockEntity be,float pt,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var state=be.getBlockState();var facing=state.getValue(ReciprocatingFluidPumpBlock.FACING);
        pose.pushPose();pose.translate(.5,.5,.5);
        float yaw=switch(facing){case EAST->-90;case SOUTH->180;case WEST->90;default->0;};
        pose.mulPose(Axis.YP.rotationDegrees(yaw));pose.translate(-.5,-.5,-.5);
        double angle=be.visualPhase(pt);
        double pinY=24-2*Math.cos(angle),pinZ=8+2*Math.sin(angle);
        double slider=pinY-Math.sqrt(16-(pinZ-8)*(pinZ-8));
        double offset=(slider-18)/16;
        pose.pushPose();pose.translate(0,offset,0);draw(PISTON,be,pose,buffers,light);pose.popPose();
        pose.pushPose();pose.translate(.5,24/16d,.5);pose.mulPose(Axis.XP.rotation((float)-angle));pose.translate(-.5,-24/16d,-.5);draw(CRANK,be,pose,buffers,light);pose.popPose();
        pose.pushPose();pose.translate(10.5/16,slider/16,.5);pose.mulPose(Axis.XP.rotation((float)Math.asin((pinZ-8)/4)));pose.translate(-10.5/16,-18/16d,-.5);draw(LINK,be,pose,buffers,light);pose.popPose();
        pose.popPose();
        var axis=((ReciprocatingFluidPumpBlock)state.getBlock()).getRotationAxis(state);
        pose.pushPose();pose.translate(0,1,0);
        var drive=be.getLevel().getBlockEntity(be.getBlockPos().above());
        if(drive instanceof com.simibubi.create.content.kinetics.base.KineticBlockEntity kinetic)
            for(var side:Direction.values())if(side.getAxis()==axis)KineticBlockEntityRenderer.standardKineticRotationTransform(CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF,state,side),kinetic,light).renderInto(pose,buffers.getBuffer(RenderType.solid()));
        pose.popPose();
    }
}
