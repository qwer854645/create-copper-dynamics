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
    private int remainingOutput(){if(level==null)return 0;if(budgetTick!=level.getGameTime()){budgetTick=level.getGameTime();outputUsed=0;}return Math.max(0,Math.max(1,(int)(Math.abs(getSpeed())/2))-outputUsed);}
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
            public int fill(FluidStack fluid,FluidAction action){return mode==OmniPumpBlock.INPUT&&allowed(mode)?buffer.fill(fluid,action):0;}
            public FluidStack drain(int amount,FluidAction action){
                if(mode!=OmniPumpBlock.OUTPUT||!allowed(mode)||!powered())return FluidStack.EMPTY;
                var drained=buffer.drain(Math.min(amount,remainingOutput()),action);if(action.execute())outputUsed+=drained.getAmount();return drained;
            }
            public FluidStack drain(FluidStack fluid,FluidAction action){return FluidStack.isSameFluidSameComponents(fluid,buffer.getFluid())?drain(fluid.getAmount(),action):FluidStack.EMPTY;}
        };
    }
    private record Step(BlockPos pos,Direction entry,int distance){}
    private record Endpoint(IFluidHandler handler,BlockPos pos,java.util.Map<BlockPos,BlockPos> parents){}
    private java.util.List<Endpoint> endpoints(Direction port,boolean input){
        var result=new java.util.ArrayList<Endpoint>();var queue=new java.util.ArrayDeque<Step>();
        var parents=new java.util.HashMap<BlockPos,BlockPos>();var visited=new java.util.HashSet<BlockPos>();visited.add(worldPosition);
        var first=worldPosition.relative(port);queue.add(new Step(first,port.getOpposite(),1));parents.put(first,worldPosition);
        int range=dev.ccd.CCDConfig.boosterRange(FluidPropagator.getPumpRange());
        while(!queue.isEmpty()&&visited.size()<4096){
            var step=queue.removeFirst();if(step.distance>range||!level.hasChunkAt(step.pos)||!visited.add(step.pos))continue;
            if(level.getBlockEntity(step.pos) instanceof PumpBlockEntity || level.getBlockEntity(step.pos) instanceof OmniPumpBlockEntity)continue;
            var pipe=FluidPropagator.getPipe(level,step.pos);var state=level.getBlockState(step.pos);
            if(pipe!=null){
                if(!pipe.canHaveFlowToward(state,step.entry))continue;
                if(!buffer.isEmpty()&&!pipe.canPullFluidFrom(buffer.getFluid(),state,step.entry))continue;
                for(var side:Direction.values())if(side!=step.entry&&pipe.canHaveFlowToward(state,side)){
                    var next=step.pos.relative(side);if(visited.contains(next)||parents.containsKey(next))continue;
                    parents.put(next,step.pos);queue.add(new Step(next,side.getOpposite(),step.distance+1));
                }
            }else{
                var cap=level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,step.pos,step.entry);
                if(cap!=null)result.add(new Endpoint(cap,step.pos,parents));
            }
        }
        return result;
    }
    @Override public void tick(){
        super.tick();if(level==null||level.isClientSide()||!powered())return;
        int rate=Math.max(1,(int)(Math.abs(getSpeed())/2));int inputBudget=rate,outputBudget=remainingOutput();
        // Rotate face priority so multiple ports share the budget over time.
        var sides=Direction.values();int offset=(int)(level.getGameTime()%sides.length);
        var seen=new java.util.HashSet<IFluidHandler>();
        for(int i=0;i<sides.length&&inputBudget>0;i++){
            var side=sides[(i+offset)%sides.length];if(OmniPumpBlock.mode(getBlockState(),side)!=OmniPumpBlock.INPUT)continue;
            for(var endpoint:endpoints(side,true)){
                if(!seen.add(endpoint.handler))continue;
                int room=Math.min(inputBudget,buffer.getCapacity()-buffer.getFluidAmount());if(room<=0)break;
                var sample=buffer.isEmpty()?endpoint.handler.drain(room,FluidAction.SIMULATE):endpoint.handler.drain(buffer.getFluid().copyWithAmount(room),FluidAction.SIMULATE);
                int accepted=buffer.fill(sample,FluidAction.SIMULATE);if(accepted<=0)continue;
                // Check every filter along the selected input route against the actual sampled fluid.
                boolean valid=true;
                for(var pos=endpoint.pos;pos!=null&&!pos.equals(worldPosition);pos=endpoint.parents.get(pos)){
                    var pipe=FluidPropagator.getPipe(level,pos);if(pipe==null)continue;
                    var prior=endpoint.parents.get(pos);var entry=Direction.getNearest(prior.getX()-pos.getX(),prior.getY()-pos.getY(),prior.getZ()-pos.getZ());
                    if(!pipe.canPullFluidFrom(sample,level.getBlockState(pos),entry)){valid=false;break;}
                }
                if(!valid)continue;
                var drained=endpoint.handler.drain(sample.copyWithAmount(accepted),FluidAction.EXECUTE);
                int filled=buffer.fill(drained,FluidAction.EXECUTE);inputBudget-=filled;
                FlowMeterBlockEntity.recordPath(level,endpoint.pos,endpoint.parents,filled);
            }
        }
        seen.clear();
        for(int i=0;i<sides.length&&outputBudget>0&&!buffer.isEmpty();i++){
            var side=sides[(i+offset)%sides.length];if(OmniPumpBlock.mode(getBlockState(),side)!=OmniPumpBlock.OUTPUT)continue;
            for(var endpoint:endpoints(side,false)){
                if(!seen.add(endpoint.handler)||buffer.isEmpty())continue;
                var offer=buffer.drain(outputBudget,FluidAction.SIMULATE);
                int filled=Math.clamp(endpoint.handler.fill(offer,FluidAction.EXECUTE),0,offer.getAmount());
                buffer.drain(filled,FluidAction.EXECUTE);outputBudget-=filled;outputUsed+=filled;
                FlowMeterBlockEntity.recordPath(level,endpoint.pos,endpoint.parents,filled);
            }
        }
    }
    @Override protected void write(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries,boolean packet){super.write(tag,registries,packet);tag.put("Buffer",buffer.writeToNBT(registries,new net.minecraft.nbt.CompoundTag()));}
    @Override protected void read(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries,boolean packet){super.read(tag,registries,packet);buffer.readFromNBT(registries,tag.getCompound("Buffer"));}
    @Override public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip,boolean sneaking){
        tooltip.add(net.minecraft.network.chat.Component.translatable("block.ccd.omni_pump"));
        for(var side:Direction.values())tooltip.add(net.minecraft.network.chat.Component.translatable("ccd.omni_pump.face",net.minecraft.network.chat.Component.translatable("ccd.direction."+side.getName()),net.minecraft.network.chat.Component.translatable("ccd.omni_pump.mode."+OmniPumpBlock.mode(getBlockState(),side))));
        return true;
    }
}
