package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.ccd.content.OmniPumpBlock;
import dev.ccd.content.OmniPumpBlockEntity;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

public final class OmniPumpRenderer extends SafeBlockEntityRenderer<OmniPumpBlockEntity>{
    public OmniPumpRenderer(BlockEntityRendererProvider.Context context){}
    @Override protected void renderSafe(OmniPumpBlockEntity be,float pt,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var state=be.getBlockState();
        PumpContainerAttachmentRenderer.render(be,pose,buffers,light,overlay);
        for(var side:Direction.values())if(OmniPumpBlock.mode(state,side)==OmniPumpBlock.SHAFT){
            float offset=KineticBlockEntityRenderer.getRotationOffsetForPosition(be,be.getBlockPos(),side.getAxis());
            float angle=(float)Math.toRadians((net.createmod.catnip.animation.AnimationTickHolder.getRenderTime(be.getLevel())*be.getSpeed()*.3f+offset)%360);
            KineticBlockEntityRenderer.kineticRotationTransform(CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF,state,side),be,side.getAxis(),angle,light).renderInto(pose,buffers.getBuffer(RenderType.solid()));
        }
    }
}
