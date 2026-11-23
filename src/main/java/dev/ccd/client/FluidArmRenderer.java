package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class FluidArmRenderer extends ArmRenderer {
    public FluidArmRenderer(BlockEntityRendererProvider.Context context){super(context);}
    @Override protected void renderSafe(ArmBlockEntity arm,float pt,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        super.renderSafe(arm,pt,pose,buffers,light,overlay);
        com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer.renderOnBlockEntity(arm,pt,pose,buffers,light,overlay);
    }
}
