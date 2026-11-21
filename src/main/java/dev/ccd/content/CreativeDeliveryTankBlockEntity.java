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
    @Override public void addBehaviours(java.util.List<com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour> behaviours){
        behaviours.add(worldOutput=new com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour(
            net.minecraft.network.chat.Component.translatable("ccd.creative_delivery.mode"),this,
            new CreativeDeliverySlotTransform()){
                @Override public com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard createBoard(net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){
                    return new com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard(
                        net.minecraft.network.chat.Component.translatable("ccd.creative_delivery.mode"),1,1,
                        java.util.List.of(net.minecraft.network.chat.Component.translatable("ccd.creative_delivery.mode")),
                        new com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter(setting->modeName(setting.value())));
                }
            }.between(0,1).withFormatter(value->net.minecraft.network.chat.Component.translatable(
                value==0?"ccd.creative_delivery.containers_only":"ccd.creative_delivery.allow_world").getString()));
    }
    private static net.minecraft.network.chat.MutableComponent modeName(int value){return net.minecraft.network.chat.Component.translatable(value==0?"ccd.creative_delivery.containers_only":"ccd.creative_delivery.allow_world");}
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
        dev.ccd.CCDTooltip.heading(tooltip,"create.gui.goggles.fluid_container");
        if(selected.isEmpty())dev.ccd.CCDTooltip.line(tooltip,"ccd.creative_delivery.empty");
        else{
            com.simibubi.create.foundation.utility.CreateLang.fluidName(selected).style(net.minecraft.ChatFormatting.GRAY).forGoggles(tooltip,1);
            dev.ccd.CCDTooltip.line(tooltip,net.minecraft.ChatFormatting.GOLD,"ccd.creative_delivery.active");
        }
        dev.ccd.CCDTooltip.line(tooltip,"ccd.goggles.output_mode",modeName(worldOutput.getValue()));return true;
    }
}
