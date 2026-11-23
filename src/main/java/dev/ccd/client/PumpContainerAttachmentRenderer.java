package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.ccd.content.*;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;

/** Native container plate only: CCD models already supply their rim and connector. */
public final class PumpContainerAttachmentRenderer {
    private PumpContainerAttachmentRenderer(){}
    public static void render(SmartBlockEntity owner,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var level=owner.getLevel();if(level==null)return;
        var state=owner.getBlockState();
        var transport=owner.getBehaviour(FluidTransportBehaviour.TYPE);
        if(transport==null)return;
        for(var side:Direction.values()){
            boolean open;
            if(owner instanceof OmniPumpBlockEntity){
                int mode=OmniPumpBlock.mode(state,side);open=mode==OmniPumpBlock.INPUT||mode==OmniPumpBlock.OUTPUT;
            }else open=PumpPlacement.mouth(state,side);
            if(!open)continue;
            var neighbor=owner.getBlockPos().relative(side);
            if(!level.hasChunkAt(neighbor))continue;
            if(transport.getRenderedRimAttachment(level,owner.getBlockPos(),state,side)!=FluidTransportBehaviour.AttachmentTypes.DRAIN)continue;
            var model=AllPartialModels.PIPE_ATTACHMENTS.get(FluidTransportBehaviour.AttachmentTypes.ComponentPartials.DRAIN).get(side);
            CachedBuffers.partial(model,state).light(light).overlay(overlay).renderInto(pose,buffers.getBuffer(RenderType.cutoutMipped()));
        }
    }
}
