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
import net.neoforged.neoforge.capabilities.Capabilities;

/** Native container plate only: CCD models already supply their rim and connector. */
public final class PumpContainerAttachmentRenderer {
    private PumpContainerAttachmentRenderer(){}
    public static void render(SmartBlockEntity owner,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var level=owner.getLevel();if(level==null)return;
        var state=owner.getBlockState();
        for(var side:Direction.values()){
            boolean open;
            if(owner instanceof OmniPumpBlockEntity){
                int mode=OmniPumpBlock.mode(state,side);open=mode==OmniPumpBlock.INPUT||mode==OmniPumpBlock.OUTPUT;
            }else open=PumpPlacement.mouth(state,side);
            if(!open)continue;
            var neighbor=owner.getBlockPos().relative(side);
            if(com.simibubi.create.AllBlocks.HOSE_PULLEY.has(level.getBlockState(neighbor)))continue;
            if(!level.hasChunkAt(neighbor)||level.getCapability(Capabilities.FluidHandler.BLOCK,neighbor,side.getOpposite())==null)continue;
            var model=AllPartialModels.PIPE_ATTACHMENTS.get(FluidTransportBehaviour.AttachmentTypes.ComponentPartials.DRAIN).get(side);
            CachedBuffers.partial(model,state).light(light).overlay(overlay).renderInto(pose,buffers.getBuffer(RenderType.cutoutMipped()));
        }
    }
}
