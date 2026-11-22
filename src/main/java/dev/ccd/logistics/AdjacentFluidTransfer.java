package dev.ccd.logistics;

import net.minecraft.core.Direction;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Constant-cost transfer for directly touching endpoints; pipes are always owned by Create. */
public final class AdjacentFluidTransfer {
    private AdjacentFluidTransfer(){}
    public static void output(SmartBlockEntity owner,Direction side,IFluidHandler source,int rate,PumpOpenEnds ends){
        var level=owner.getLevel();var pos=owner.getBlockPos().relative(side);
        if(!level.hasChunkAt(pos)||com.simibubi.create.content.fluids.FluidPropagator.getPipe(level,pos)!=null)return;
        var target=level.getCapability(Capabilities.FluidHandler.BLOCK,pos,side.getOpposite());
        if(target==null&&ends!=null)target=ends.handler(owner,pos,side.getOpposite());
        if(target==null||target==source)return;
        var offer=source.drain(rate,IFluidHandler.FluidAction.SIMULATE);if(offer.isEmpty())return;
        int accepted=Math.clamp(target.fill(offer,IFluidHandler.FluidAction.SIMULATE),0,offer.getAmount());
        if(accepted==0)return;
        var actual=source.drain(offer.copyWithAmount(accepted),IFluidHandler.FluidAction.EXECUTE);
        if(actual.isEmpty())return;
        int filled=Math.clamp(target.fill(actual,IFluidHandler.FluidAction.EXECUTE),0,actual.getAmount());
        if(filled<actual.getAmount())source.fill(actual.copyWithAmount(actual.getAmount()-filled),IFluidHandler.FluidAction.EXECUTE);
    }
    public static void input(SmartBlockEntity owner,Direction side,IFluidHandler target,int rate){
        var level=owner.getLevel();var pos=owner.getBlockPos().relative(side);
        if(!level.hasChunkAt(pos)||com.simibubi.create.content.fluids.FluidPropagator.getPipe(level,pos)!=null)return;
        var source=level.getCapability(Capabilities.FluidHandler.BLOCK,pos,side.getOpposite());
        if(source==null||source==target)return;
        var offer=source.drain(rate,IFluidHandler.FluidAction.SIMULATE);
        int accepted=target.fill(offer,IFluidHandler.FluidAction.SIMULATE);
        if(accepted<=0)return;
        var actual=source.drain(offer.copyWithAmount(accepted),IFluidHandler.FluidAction.EXECUTE);
        int filled=target.fill(actual,IFluidHandler.FluidAction.EXECUTE);
        if(filled<actual.getAmount())source.fill(actual.copyWithAmount(actual.getAmount()-filled),IFluidHandler.FluidAction.EXECUTE);
    }
}
