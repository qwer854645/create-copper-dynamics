package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ccd.content.CreativeDeliveryTankBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

public final class CreativeDeliveryTankRenderer extends SafeBlockEntityRenderer<CreativeDeliveryTankBlockEntity>{
    public CreativeDeliveryTankRenderer(BlockEntityRendererProvider.Context context){}
    @Override protected void renderSafe(CreativeDeliveryTankBlockEntity be,float pt,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var fluid=be.displayFluid();if(fluid.isEmpty())return;
        net.createmod.catnip.platform.NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluid,
            3/16f,2/16f,3/16f,13/16f,14/16f,13/16f,buffers,pose,light,false,true);
    }
}
