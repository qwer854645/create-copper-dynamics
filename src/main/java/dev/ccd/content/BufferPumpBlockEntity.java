package dev.ccd.content;

import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Finite buffer supplied externally; rotational power only pumps stored fluid outward. */
public final class BufferPumpBlockEntity extends PumpBlockEntity {
    public static final int CAPACITY=2000;
    public com.simibubi.create.foundation.fluid.SmartFluidTank buffer;
    public BufferPumpBlockEntity(BlockPos pos,BlockState state){super(CopperDynamics.BUFFER_PUMP_BE.get(),pos,state);}
    public IFluidHandler handler(Direction side){
        if(side==null)return buffer;
        return new IFluidHandler(){
            public int getTanks(){return 1;}
            public FluidStack getFluidInTank(int tank){return buffer.getFluid().copy();}
            public int getTankCapacity(int tank){return CAPACITY;}
            public boolean isFluidValid(int tank,FluidStack fluid){return true;}
            public int fill(FluidStack fluid,FluidAction action){return buffer.fill(fluid,action);}
            public FluidStack drain(int amount,FluidAction action){return side==getBlockState().getValue(BufferPumpBlock.FACING)&&getSpeed()!=0&&!isOverStressed()?buffer.drain(amount,action):FluidStack.EMPTY;}
            public FluidStack drain(FluidStack fluid,FluidAction action){return FluidStack.isSameFluidSameComponents(fluid,buffer.getFluid())?drain(fluid.getAmount(),action):FluidStack.EMPTY;}
        };
    }
    @Override public void addBehaviours(java.util.List<com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour> behaviours){
        super.addBehaviours(behaviours);
        var storage=com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.single(this,CAPACITY);
        behaviours.add(storage);buffer=storage.getPrimaryHandler();
        // This device owns finite storage and delivers from it directly. Running
        // the native transparent pump transport creates an invalid empty-port lifecycle.
        behaviours.removeIf(behaviour->behaviour instanceof com.simibubi.create.content.fluids.FluidTransportBehaviour);
        behaviours.add(new dev.ccd.logistics.StoredSourceTransport(this,()->getBlockState().getValue(BufferPumpBlock.FACING),()->handler(getBlockState().getValue(BufferPumpBlock.FACING)),()->isOverStressed()?0f:Math.abs(getSpeed())));
    }
    @Override public boolean isSideAccessible(Direction side){return false;}
    @Override public void updatePipesOnSide(Direction side){if(!dev.ccd.logistics.NativeEndpointPump.UPDATING.get())invalidatePressure();} // Endpoint pressure only.
    @Override public void updatePressureChange(){invalidatePressure();} // No transparent bypass around the finite buffer.
    public int pumpingRange(){return dev.ccd.CCDConfig.bufferPumpRange(com.simibubi.create.content.fluids.FluidPropagator.getPumpRange());}
    private boolean pressureDirty=true;
    public void invalidatePressure(){pressureDirty=true;}
    @Override public void onSpeedChanged(float previous){super.onSpeedChanged(previous);invalidatePressure();}
    @Override public void tick(){
        super.tick();if(level==null||level.isClientSide())return;
        if(!pressureDirty)return;
        pressureDirty=false;
        var port=getBlockState().getValue(BufferPumpBlock.FACING);
        var neighbor=worldPosition.relative(port);
        dev.ccd.logistics.NativeEndpointPump.changed(level,neighbor);
        new dev.ccd.logistics.NativeEndpointPump(level,worldPosition,getBlockState()).distribute(port,false,isOverStressed()?0:Math.abs(getSpeed()));
    }
    @Override public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip,boolean sneaking){super.addToGoggleTooltip(tooltip,sneaking);dev.ccd.CCDTooltip.fluidContents(tooltip,buffer.isEmpty()?java.util.List.of():java.util.List.of(buffer.getFluid()),CAPACITY);return true;}
}
