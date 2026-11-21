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

public final class FrequencyPumpRenderer extends SafeBlockEntityRenderer<ReciprocatingFluidPumpBlockEntity> {
    private static final PartialModel COG=PartialModel.of(ResourceLocation.fromNamespaceAndPath("ccd","block/frequency_pump/cog"));
    public static void registerModels(){}
    public FrequencyPumpRenderer(BlockEntityRendererProvider.Context context){}
    @Override protected void renderSafe(ReciprocatingFluidPumpBlockEntity be,float pt,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var state=be.getBlockState();var front=state.getValue(ReciprocatingFluidPumpBlock.FACING);
        float time=net.createmod.catnip.animation.AnimationTickHolder.getRenderTime(be.getLevel());
        float angle=(float)Math.toRadians((time*be.getSpeed()*.3f)%360);
        pose.pushPose();pose.translate(.5,.5,.5);
        pose.mulPose(Axis.YP.rotationDegrees(switch(front){case EAST->-90;case SOUTH->180;case WEST->90;default->0;}));
        pose.mulPose(Axis.XP.rotation(angle));pose.translate(-.5,-.5,-.5);
        CachedBuffers.partial(COG,state).light(light).overlay(overlay).renderInto(pose,buffers.getBuffer(RenderType.solid()));pose.popPose();
    }
}
