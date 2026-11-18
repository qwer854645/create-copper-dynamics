package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.ccd.content.FaucetBlockEntity;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Real fluid texture stream after a successful transfer; purely visual, no extra liquid inventory. */
public final class FaucetRenderer extends SmartBlockEntityRenderer<FaucetBlockEntity> {
    public FaucetRenderer(BlockEntityRendererProvider.Context context) { super(context); }
    @Override protected void renderSafe(FaucetBlockEntity entity,float partialTicks,PoseStack pose,
        MultiBufferSource buffer,int light,int overlay) {
        super.renderSafe(entity,partialTicks,pose,buffer,light,overlay);
        var fluid=entity.flowingFluid();
        if(fluid.isEmpty()) return;
        NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluid,.455f,.1f-entity.flowDistance(),.455f,
            .545f,.065f,.545f,buffer,pose,light,false,true);
    }
}
