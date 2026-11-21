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
    private final dev.ccd.logistics.PumpOpenEnds openEnds=new dev.ccd.logistics.PumpOpenEnds();
    public final net.neoforged.neoforge.fluids.capability.templates.FluidTank buffer=new net.neoforged.neoforge.fluids.capability.templates.FluidTank(CAPACITY){
        @Override protected void onContentsChanged(){setChanged();if(level!=null&&!level.isClientSide())sendData();}
    };
    public BufferPumpBlockEntity(BlockPos pos,BlockState state){super(CopperDynamics.BUFFER_PUMP_BE.get(),pos,state);}
    @Override public void addBehaviours(java.util.List<com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour> behaviours){
        super.addBehaviours(behaviours);
        // This device owns finite storage and delivers from it directly. Running
        // the native transparent pump transport creates an invalid empty-port lifecycle.
        behaviours.removeIf(behaviour->behaviour instanceof com.simibubi.create.content.fluids.FluidTransportBehaviour);
    }
    @Override public boolean isSideAccessible(Direction side){return false;}
    @Override public void updatePipesOnSide(Direction side){} // No native transport behaviour to invalidate.
    @Override public void updatePressureChange(){} // No transparent bypass around the finite buffer.
    public int pumpingRange(){return dev.ccd.CCDConfig.bufferPumpRange(com.simibubi.create.content.fluids.FluidPropagator.getPumpRange());}
    private record Step(BlockPos pos,Direction entry,int distance){}
    @Override public void tick(){
        super.tick();if(level==null||level.isClientSide()||getSpeed()==0||isOverStressed())return;
        if(buffer.isEmpty())return;
        int remaining=Math.max(1,(int)(Math.abs(getSpeed())/2));
        var queue=new java.util.ArrayDeque<Step>();var visited=new java.util.HashSet<BlockPos>();
        Direction output=getBlockState().getValue(BufferPumpBlock.FACING);
        queue.add(new Step(worldPosition.relative(output),output.getOpposite(),1));visited.add(worldPosition);
        int range=pumpingRange();
        while(!queue.isEmpty()&&remaining>0&&!buffer.isEmpty()&&visited.size()<4096){
            var step=queue.removeFirst();
            if(step.distance>range||!level.hasChunkAt(step.pos)||!visited.add(step.pos))continue;
            var state=level.getBlockState(step.pos);
            var pipe=com.simibubi.create.content.fluids.FluidPropagator.getPipe(level,step.pos);
            if(pipe!=null){
                if(!pipe.canHaveFlowToward(state,step.entry)||!pipe.canPullFluidFrom(buffer.getFluid(),state,step.entry))continue;
                if(level.getBlockEntity(step.pos) instanceof PumpBlockEntity)continue;
                for(var side:Direction.values())if(side!=step.entry&&pipe.canHaveFlowToward(state,side))
                    queue.add(new Step(step.pos.relative(side),side.getOpposite(),step.distance+1));
                continue;
            }
            var target=level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,step.pos,step.entry);
            if(target==null)target=openEnds.handler(this,step.pos,step.entry);
            if(target==null||target==buffer)continue;
            var offer=buffer.drain(remaining,IFluidHandler.FluidAction.SIMULATE);
            int accepted=target.fill(offer,IFluidHandler.FluidAction.SIMULATE);
            if(accepted>0){var drained=buffer.drain(Math.min(accepted,offer.getAmount()),IFluidHandler.FluidAction.EXECUTE);int filled=Math.clamp(target.fill(drained,IFluidHandler.FluidAction.EXECUTE),0,drained.getAmount());remaining-=filled;if(filled<drained.getAmount())buffer.fill(drained.copyWithAmount(drained.getAmount()-filled),IFluidHandler.FluidAction.EXECUTE);}
        }
    }
    @Override protected void write(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries,boolean packet){super.write(tag,registries,packet);tag.put("Buffer",buffer.writeToNBT(registries,new net.minecraft.nbt.CompoundTag()));openEnds.write(tag,registries);}
    @Override protected void read(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries,boolean packet){super.read(tag,registries,packet);buffer.readFromNBT(registries,tag.getCompound("Buffer"));openEnds.read(tag,registries);}
    @Override public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip,boolean sneaking){super.addToGoggleTooltip(tooltip,sneaking);if(!buffer.isEmpty())tooltip.add(buffer.getFluid().getHoverName());tooltip.add(net.minecraft.network.chat.Component.literal(buffer.getFluidAmount()+" / "+CAPACITY+" mB"));return true;}
}
