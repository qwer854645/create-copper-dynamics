package dev.ccd.client;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class TankBlazeBurnerRenderer extends BlazeBurnerRenderer {
    public static void registerModels(){}
    public TankBlazeBurnerRenderer(BlockEntityRendererProvider.Context context){super(context);}
    @Override protected void renderSafe(BlazeBurnerBlockEntity be,float pt,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        // Preserve native size, placement and heat-dependent head/rod animation.
        super.renderSafe(be,pt,pose,buffers,light,overlay);
        if(be instanceof dev.ccd.content.TankBlazeBurnerBlockEntity burner&&!burner.tank.isEmpty())
            net.createmod.catnip.platform.NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(burner.tank.getFluid(),1.1f/16,1.1f/16,1.1f/16,14.9f/16,(1.1f+2.8f*Math.min(1f,burner.tank.getFluidAmount()/4000f))/16,14.9f/16,buffers,pose,light,false,true);
    }
}
