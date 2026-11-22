package dev.ccd.content;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** One shared finite buffer and one per-tick delivery budget for all configured ports. */
public final class OmniPumpBlockEntity extends KineticBlockEntity implements com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation {
    private long budgetTick=Long.MIN_VALUE;
    private int outputUsed;
    private int inputUsed;
    private int remainingOutput(){resetBudget();return Math.max(0,Math.max(1,(int)(Math.abs(getSpeed())/2))-outputUsed);}
    private void resetBudget(){if(level!=null&&budgetTick!=level.getGameTime()){budgetTick=level.getGameTime();outputUsed=0;inputUsed=0;}}
    public final net.neoforged.neoforge.fluids.capability.templates.FluidTank buffer=new net.neoforged.neoforge.fluids.capability.templates.FluidTank(2000){
        @Override protected void onContentsChanged(){setChanged();}
    };
    public OmniPumpBlockEntity(BlockPos pos,BlockState state){super(CopperDynamics.OMNI_PUMP_BE.get(),pos,state);}
    private boolean powered(){return getSpeed()!=0&&!isOverStressed();}
    public IFluidHandler handler(Direction side){
        if(side==null)return null;int mode=OmniPumpBlock.mode(getBlockState(),side);
        if(mode!=OmniPumpBlock.INPUT&&mode!=OmniPumpBlock.OUTPUT)return null;
        return new IFluidHandler(){
            public int getTanks(){return 1;}
            public FluidStack getFluidInTank(int tank){return buffer.getFluid().copy();}
            public int getTankCapacity(int tank){return buffer.getCapacity();}
            public boolean isFluidValid(int tank,FluidStack fluid){return mode==OmniPumpBlock.INPUT&&buffer.isFluidValid(tank,fluid);}
            private boolean allowed(int expected){return !isRemoved()&&OmniPumpBlock.mode(getBlockState(),side)==expected;}
            public int fill(FluidStack fluid,FluidAction action){
                if(mode!=OmniPumpBlock.INPUT||!allowed(mode))return 0;
                resetBudget();int remaining=Math.max(0,Math.max(1,(int)(Math.abs(getSpeed())/2))-inputUsed);
                int accepted=buffer.fill(fluid.copyWithAmount(Math.min(remaining,fluid.getAmount())),action);
                if(action.execute())inputUsed+=accepted;return accepted;
            }
            public FluidStack drain(int amount,FluidAction action){
                if(mode!=OmniPumpBlock.OUTPUT||!allowed(mode)||!powered())return FluidStack.EMPTY;
                var drained=buffer.drain(Math.min(amount,remainingOutput()),action);if(action.execute())outputUsed+=drained.getAmount();return drained;
            }
            public FluidStack drain(FluidStack fluid,FluidAction action){return FluidStack.isSameFluidSameComponents(fluid,buffer.getFluid())?drain(fluid.getAmount(),action):FluidStack.EMPTY;}
        };
    }
    private boolean pressureDirty=true;
    public void invalidatePressure(){pressureDirty=true;}
    @Override public void onSpeedChanged(float previous){super.onSpeedChanged(previous);invalidatePressure();}
    @Override public void tick(){
        super.tick();if(level==null||level.isClientSide())return;
        if(powered())for(var side:Direction.values()){
            int mode=OmniPumpBlock.mode(getBlockState(),side);
            if(mode==OmniPumpBlock.INPUT)dev.ccd.logistics.AdjacentFluidTransfer.input(this,side,handler(side),Math.max(1,(int)(Math.abs(getSpeed())/2)));
            if(mode==OmniPumpBlock.OUTPUT)dev.ccd.logistics.AdjacentFluidTransfer.output(this,side,handler(side),remainingOutput(),null);
        }
        if(!pressureDirty)return;
        pressureDirty=false;
        var adapter=new dev.ccd.logistics.NativeEndpointPump(level,worldPosition,getBlockState());
        for(var side:Direction.values()){
            var next=worldPosition.relative(side);
            dev.ccd.logistics.NativeEndpointPump.changed(level,next);
        }
        for(var side:Direction.values()){
            int mode=OmniPumpBlock.mode(getBlockState(),side);
            if(mode==OmniPumpBlock.INPUT||mode==OmniPumpBlock.OUTPUT)adapter.distribute(side,mode==OmniPumpBlock.INPUT,powered()?Math.abs(getSpeed()):0);
        }
    }
    @Override protected void write(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries,boolean packet){super.write(tag,registries,packet);tag.put("Buffer",buffer.writeToNBT(registries,new net.minecraft.nbt.CompoundTag()));}
    @Override protected void read(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries,boolean packet){super.read(tag,registries,packet);buffer.readFromNBT(registries,tag.getCompound("Buffer"));}
    @Override public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip,boolean sneaking){
        super.addToGoggleTooltip(tooltip,sneaking);
        dev.ccd.CCDTooltip.heading(tooltip,"ccd.goggles.connections");
        boolean configured=false;
        for(var side:Direction.values())if(OmniPumpBlock.mode(getBlockState(),side)!=OmniPumpBlock.CLOSED){
            configured=true;
            dev.ccd.CCDTooltip.line(tooltip,"ccd.omni_pump.face",net.minecraft.network.chat.Component.translatable("ccd.direction."+side.getName()),net.minecraft.network.chat.Component.translatable("ccd.omni_pump.mode."+OmniPumpBlock.mode(getBlockState(),side)).withStyle(net.minecraft.ChatFormatting.AQUA));
        }
        if(!configured)dev.ccd.CCDTooltip.line(tooltip,"ccd.goggles.no_connections");
        return true;
    }
}
