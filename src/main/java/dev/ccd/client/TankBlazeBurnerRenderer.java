package dev.ccd.client;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class TankBlazeBurnerRenderer extends BlazeBurnerRenderer {
    public TankBlazeBurnerRenderer(BlockEntityRendererProvider.Context context){super(context);}
    @Override protected void renderSafe(BlazeBurnerBlockEntity be,float pt,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(0,.25,0);pose.scale(1,.75f,1);
        super.renderSafe(be,pt,pose,buffers,light,overlay);pose.popPose();
        if(be instanceof dev.ccd.content.TankBlazeBurnerBlockEntity burner&&!burner.tank.isEmpty())
            net.createmod.catnip.platform.NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(burner.tank.getFluid(),1/16f,1/16f,1/16f,15/16f,(1+2f*burner.tank.getFluidAmount()/4000)/16,15/16f,buffers,pose,light,false,true);
    }
}
