package dev.ccd.logistics;

import net.minecraft.core.Direction;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Constant-cost transfer for directly touching endpoints; pipes are always owned by Create. */
public final class AdjacentFluidTransfer {
    private AdjacentFluidTransfer(){}
    /** Returns owned fluid which neither endpoint accepted; caller must preserve it. */
    public static net.neoforged.neoforge.fluids.FluidStack transfer(IFluidHandler source,IFluidHandler target,int rate){
        if(rate<=0||source==target)return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
        var offer=source.drain(rate,IFluidHandler.FluidAction.SIMULATE);
        if(offer.isEmpty())return offer;
        int accepted=Math.clamp(target.fill(offer.copy(),IFluidHandler.FluidAction.SIMULATE),0,Math.min(rate,offer.getAmount()));
        if(accepted==0)return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
        var actual=source.drain(offer.copyWithAmount(accepted),IFluidHandler.FluidAction.EXECUTE);
        if(actual.isEmpty())return actual;
        int filled=0;
        if(actual.getAmount()<=accepted&&net.neoforged.neoforge.fluids.FluidStack.isSameFluidSameComponents(actual,offer))
            filled=Math.clamp(target.fill(actual.copy(),IFluidHandler.FluidAction.EXECUTE),0,actual.getAmount());
        var rest=actual.copyWithAmount(actual.getAmount()-filled);
        if(rest.isEmpty())return rest;
        int returned=Math.clamp(source.fill(rest.copy(),IFluidHandler.FluidAction.EXECUTE),0,rest.getAmount());
        return rest.copyWithAmount(rest.getAmount()-returned);
    }
    public static void recover(SmartBlockEntity owner,net.neoforged.neoforge.fluids.FluidStack rest){
        if(rest.isEmpty())return;
        // Output wrappers are deliberately drain-only. Restore the actual owned buffer,
        // without treating a rollback as another port input or resetting delivery budgets.
        if(owner instanceof dev.ccd.content.OmniPumpBlockEntity pump){
            int restored=pump.buffer.fill(rest.copy(),IFluidHandler.FluidAction.EXECUTE);
            rest=rest.copyWithAmount(rest.getAmount()-Math.clamp(restored,0,rest.getAmount()));
        }
        if(!rest.isEmpty())net.minecraft.world.Containers.dropItemStack(owner.getLevel(),owner.getBlockPos().getX()+.5,
            owner.getBlockPos().getY()+.5,owner.getBlockPos().getZ()+.5,dev.ccd.content.FluidPackageItem.containingFluid(rest));
    }
    public static void output(SmartBlockEntity owner,Direction side,IFluidHandler source,int rate,PumpOpenEnds ends){
        var level=owner.getLevel();var pos=owner.getBlockPos().relative(side);
        if(!level.hasChunkAt(pos)||com.simibubi.create.content.fluids.FluidPropagator.getPipe(level,pos)!=null)return;
        var target=level.getCapability(Capabilities.FluidHandler.BLOCK,pos,side.getOpposite());
        if(target==null&&ends!=null)target=ends.handler(owner,pos,side.getOpposite());
        if(target==null||target==source)return;
        var rest=transfer(source,target,rate);
        // Creative sources own no finite cargo to recover.
        if(!(owner instanceof dev.ccd.content.CreativeDeliveryTankBlockEntity))recover(owner,rest);
    }
    public static void input(SmartBlockEntity owner,Direction side,IFluidHandler target,int rate){
        var level=owner.getLevel();var pos=owner.getBlockPos().relative(side);
        if(!level.hasChunkAt(pos)||com.simibubi.create.content.fluids.FluidPropagator.getPipe(level,pos)!=null)return;
        var source=level.getCapability(Capabilities.FluidHandler.BLOCK,pos,side.getOpposite());
        if(source==null||source==target)return;
        recover(owner,transfer(source,target,rate));
    }
}
