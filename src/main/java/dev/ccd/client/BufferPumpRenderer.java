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
    public BufferPumpRenderer(BlockEntityRendererProvider.Context context){}
    @Override protected void renderSafe(BufferPumpBlockEntity be,float pt,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var state=be.getBlockState();var axis=((BufferPumpBlock)state.getBlock()).getRotationAxis(state);
        for(var side:Direction.values())if(side.getAxis()==axis)
            KineticBlockEntityRenderer.standardKineticRotationTransform(CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF,state,side),be,light).renderInto(pose,buffers.getBuffer(RenderType.solid()));
        if(!be.buffer.isEmpty())net.createmod.catnip.platform.NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(be.buffer.getFluid(),
            3/16f,1/16f,3/16f,13/16f,(1+3.5f*be.buffer.getFluidAmount()/4000)/16,13/16f,buffers,pose,light,false,true);
    }
}
