package dev.ccd.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.fluids.FluidNetwork;
import com.simibubi.create.content.fluids.FlowSource;
import com.simibubi.create.content.fluids.FluidPropagator;
import dev.ccd.content.FlowMeterBlockEntity;
import net.createmod.catnip.math.BlockFace;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FluidNetwork.class)
abstract class FlowMeterNetworkMixin {
    @org.spongepowered.asm.mixin.Unique private final java.util.Map<BlockPos, java.util.List<BlockPos>> ccd$paths = new java.util.HashMap<>();
    @org.spongepowered.asm.mixin.Unique private long ccd$pathTick = Long.MIN_VALUE;
    @Shadow Level world;
    @Shadow BlockFace start;
    @Shadow java.util.List<Pair<BlockFace,FlowSource>> targets;
    @Shadow java.util.Map<BlockPos,java.lang.ref.WeakReference<com.simibubi.create.content.fluids.FluidTransportBehaviour>> cache;
    @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;fill(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)I"))
    private int ccd$measure(IFluidHandler handler,FluidStack fluid,IFluidHandler.FluidAction action,Operation<Integer> original){
        var sourceBlock=world.getBlockEntity(start.getPos());
        if(!(sourceBlock instanceof dev.ccd.content.CreativeDeliveryTankBlockEntity))sourceBlock=world.getBlockEntity(start.getConnectedPos());
        if(sourceBlock instanceof dev.ccd.content.CreativeDeliveryTankBlockEntity tank&&tank.worldOutput.getValue()==0){
            for(var target:targets)if(target.getSecond() instanceof com.simibubi.create.content.fluids.OpenEndedPipe){
                var provider=target.getSecond().provideHandler();
                if(provider!=null&&provider.getCapability()==handler)return 0;
            }
        }
        int filled=original.call(handler,fluid,action);
        if(action.execute()&&filled>0 && cache.values().stream().anyMatch(ref->{var pipe=ref.get();return pipe!=null&&pipe.blockEntity instanceof FlowMeterBlockEntity;}))for(var target:targets){
            var provider=target.getSecond().provideHandler();
            if(provider==null||provider.getCapability()!=handler)continue;
            var end=target.getFirst().getPos();
            // Reuse only within this network tick, and validate every edge below.
            // No persistent topology cache can become stale after a valve/port edit.
            if(ccd$pathTick!=world.getGameTime()){ccd$paths.clear();ccd$pathTick=world.getGameTime();}
            var cachedPath=ccd$paths.get(end);
            if(cachedPath!=null && ccd$validPath(cachedPath)) {
                for(var pos:cachedPath)if(world.getBlockEntity(pos) instanceof FlowMeterBlockEntity meter)meter.recordTransfer(filled);
                break;
            }
            var parents=new java.util.HashMap<BlockPos,BlockPos>();var queue=new java.util.ArrayDeque<BlockPos>();
            queue.add(start.getPos());parents.put(start.getPos(),start.getPos());
            while(!queue.isEmpty()&&parents.size()<=4096){
                var pos=queue.removeFirst();
                if(pos.equals(end)){
                    var path=new java.util.ArrayList<BlockPos>();
                    while(true){
                        path.add(pos);
                        if(world.getBlockEntity(pos) instanceof FlowMeterBlockEntity meter)meter.recordTransfer(filled);
                        var previous=parents.get(pos);if(previous.equals(pos))break;pos=previous;
                    }
                    ccd$paths.put(end,java.util.List.copyOf(path));
                    break;
                }
                var pipe=FluidPropagator.getPipe(world,pos);if(pipe==null)continue;
                for(var side:Direction.values()){
                    var connection=pipe.getConnection(side);
                    if(connection==null||!connection.hasFlow())continue;
                    var flow=((PipeConnectionFlowAccessor)connection).ccd$getFlow().get();
                    if(flow.inbound||!flow.complete)continue;
                    var next=pos.relative(side);if(!world.hasChunkAt(next)||parents.containsKey(next))continue;
                    var other=FluidPropagator.getPipe(world,next);if(other==null)continue;
                    var incoming=other.getConnection(side.getOpposite());
                    if(incoming==null||!incoming.hasFlow()||!((PipeConnectionFlowAccessor)incoming).ccd$getFlow().get().inbound)continue;
                    parents.put(next,pos);queue.add(next);
                }
            }
            break;
        }
        return filled;
    }
    @org.spongepowered.asm.mixin.Unique private boolean ccd$validPath(java.util.List<BlockPos> path){
        for(int i=1;i<path.size();i++){
            var from=path.get(i);var to=path.get(i-1);
            if(!world.hasChunkAt(from)||!world.hasChunkAt(to))return false;
            var pipe=FluidPropagator.getPipe(world,from);var other=FluidPropagator.getPipe(world,to);
            if(pipe==null||other==null)return false;
            var delta=to.subtract(from);var side=Direction.getNearest(delta.getX(),delta.getY(),delta.getZ());
            var out=pipe.getConnection(side);var in=other.getConnection(side.getOpposite());
            if(out==null||in==null||!out.hasFlow()||!in.hasFlow())return false;
            var outgoing=((PipeConnectionFlowAccessor)out).ccd$getFlow().get();
            if(outgoing.inbound||!outgoing.complete||!((PipeConnectionFlowAccessor)in).ccd$getFlow().get().inbound)return false;
        }
        return true;
    }
}
