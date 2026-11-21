package dev.ccd.content;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Infinite creative source with bounded pipe traversal; no owned finite cargo. */
public final class CreativeDeliveryTankBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private FluidStack selected=FluidStack.EMPTY;
    public FluidStack displayFluid(){return selected.copy();}
    private final dev.ccd.logistics.PumpOpenEnds openEnds=new dev.ccd.logistics.PumpOpenEnds();
    public static final int RANGE=256, RATE=1000000, VISIT_LIMIT=4096;
    public CreativeDeliveryTankBlockEntity(BlockPos pos,BlockState state){super(CopperDynamics.CREATIVE_DELIVERY_TANK_BE.get(),pos,state);}
    public com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour worldOutput;
    private com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour fluidSelection;
    @Override public void addBehaviours(java.util.List<com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour> behaviours){
        behaviours.add(fluidSelection=new com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour(this,new CreativeDeliverySlotTransform()){
            @Override public void onShortInteract(net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,Direction side,net.minecraft.world.phys.BlockHitResult hit){if(player.isCreative())super.onShortInteract(player,hand,side,hit);}
        }
            .forFluids().withPredicate(stack->stack.isEmpty()||!FluidStockItem.sample(stack).isEmpty())
            .withCallback(stack->{if(level!=null&&!level.isClientSide())select(FluidStockItem.sample(stack));}));
        behaviours.add(worldOutput=new com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour(
            net.minecraft.network.chat.Component.translatable("ccd.creative_delivery.mode"),this,
            new com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided(){
                @Override protected net.minecraft.world.phys.Vec3 getSouthLocation(){return new net.minecraft.world.phys.Vec3(.5,.25,1.01);}
                @Override protected boolean isSideActive(BlockState state,Direction side){return side.getAxis()!=state.getValue(CreativeDeliveryTankBlock.FACING).getAxis();}
            }).between(0,1).withFormatter(value->net.minecraft.network.chat.Component.translatable(
                value==0?"ccd.creative_delivery.containers_only":"ccd.creative_delivery.allow_world").getString()));
    }
    public void select(FluidStack fluid){selected=fluid.isEmpty()?FluidStack.EMPTY:fluid.copyWithAmount(1000);notifyUpdate();if(level!=null)level.invalidateCapabilities(worldPosition);}
    public final IFluidHandler handler=new IFluidHandler(){
        public int getTanks(){return 1;}
        public FluidStack getFluidInTank(int tank){return selected.isEmpty()?FluidStack.EMPTY:selected.copyWithAmount(Integer.MAX_VALUE);}
        public int getTankCapacity(int tank){return Integer.MAX_VALUE;}
        public boolean isFluidValid(int tank,FluidStack fluid){return false;}
        public int fill(FluidStack fluid,FluidAction action){return 0;}
        public FluidStack drain(int amount,FluidAction action){return amount<=0||selected.isEmpty()?FluidStack.EMPTY:selected.copyWithAmount(amount);}
        public FluidStack drain(FluidStack fluid,FluidAction action){return FluidStack.isSameFluidSameComponents(fluid,selected)?drain(fluid.getAmount(),action):FluidStack.EMPTY;}
    };
    private record Step(BlockPos pos,Direction entry,int distance){}
    @Override public void tick(){
        super.tick();if(level==null||level.isClientSide()||selected.isEmpty())return;
        var front=getBlockState().getValue(CreativeDeliveryTankBlock.FACING);
        var queue=new java.util.ArrayDeque<Step>();var visited=new java.util.HashSet<BlockPos>();
        var parents=new java.util.HashMap<BlockPos,BlockPos>();
        queue.add(new Step(worldPosition.relative(front),front.getOpposite(),1));visited.add(worldPosition);parents.put(worldPosition.relative(front),worldPosition);
        int remaining=RATE;
        while(!queue.isEmpty()&&remaining>0&&visited.size()<VISIT_LIMIT){
            var step=queue.removeFirst();if(step.distance>RANGE||!level.hasChunkAt(step.pos)||!visited.add(step.pos))continue;
            var state=level.getBlockState(step.pos);var pipe=FluidPropagator.getPipe(level,step.pos);
            if(pipe!=null){
                if(!pipe.canHaveFlowToward(state,step.entry)||!pipe.canPullFluidFrom(selected,state,step.entry))continue;
                // Other pumps retain control of their own pressure and route.
                if(level.getBlockEntity(step.pos) instanceof com.simibubi.create.content.fluids.pump.PumpBlockEntity)continue;
                for(var side:Direction.values())if(side!=step.entry&&pipe.canHaveFlowToward(state,side)&&!parents.containsKey(step.pos.relative(side))){parents.put(step.pos.relative(side),step.pos);queue.add(new Step(step.pos.relative(side),side.getOpposite(),step.distance+1));}
            }else{
                if(level.getBlockEntity(step.pos) instanceof CreativeDeliveryTankBlockEntity)continue;
                var target=level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,step.pos,step.entry);
                if(target==null&&worldOutput.getValue()==1)target=openEnds.handler(this,step.pos,step.entry);
                if(target!=null){int filled=Math.clamp(target.fill(selected.copyWithAmount(remaining),IFluidHandler.FluidAction.EXECUTE),0,remaining);FlowMeterBlockEntity.recordPath(level,step.pos,parents,filled);remaining-=filled;}
            }
        }
    }
    @Override protected void write(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries,boolean packet){super.write(tag,registries,packet);tag.put("SelectedFluid",selected.saveOptional(registries));openEnds.write(tag,registries);}
    @Override protected void read(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries,boolean packet){super.read(tag,registries,packet);selected=FluidStack.parseOptional(registries,tag.getCompound("SelectedFluid"));openEnds.read(tag,registries);}
    @Override public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip,boolean sneaking){
        tooltip.add(selected.isEmpty()?net.minecraft.network.chat.Component.translatable("ccd.creative_delivery.empty"):selected.getHoverName());
        if(!selected.isEmpty())tooltip.add(net.minecraft.network.chat.Component.translatable("ccd.creative_delivery.active"));return true;
    }
}
