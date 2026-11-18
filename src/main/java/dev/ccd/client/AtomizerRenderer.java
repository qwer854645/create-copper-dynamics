package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.fan.EncasedFanRenderer;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import dev.ccd.content.FluidAtomizerBlockEntity;
import dev.ccd.content.FluidAtomizerBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
public final class AtomizerRenderer extends EncasedFanRenderer {
    public AtomizerRenderer(BlockEntityRendererProvider.Context context) { super(context); }
    @Override protected void renderSafe(EncasedFanBlockEntity entity,float pt,PoseStack pose,MultiBufferSource buffer,int light,int overlay) {
        super.renderSafe(entity,pt,pose,buffer,light,overlay);
        if(!(entity instanceof FluidAtomizerBlockEntity atomizer)) return;
        var fluid=atomizer.displayFluid(); if(fluid.isEmpty()) return;
        var facing=entity.getBlockState().getValue(FluidAtomizerBlock.FACING);
        float x=.14f,X=.86f,y=.14f,Y=.86f,z=.14f,Z=.86f;
        switch(facing) {
            case NORTH -> Z=.50f;
            case SOUTH -> z=.50f;
            case WEST -> X=.50f;
            case EAST -> x=.50f;
            case UP -> y=.50f;
            case DOWN -> Y=.50f;
        }
        Y=y+(Y-y)*Math.min(1f,fluid.getAmount()/(float)FluidAtomizerBlockEntity.CAPACITY);
        if(Y>y) NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluid,x,y,z,X,Y,Z,buffer,pose,light,false,true);
    }
}
