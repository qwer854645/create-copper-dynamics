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
    @Shadow Level world;
    @Shadow BlockFace start;
    @Shadow java.util.List<Pair<BlockFace,FlowSource>> targets;
    @Shadow java.util.Map<BlockPos,java.lang.ref.WeakReference<com.simibubi.create.content.fluids.FluidTransportBehaviour>> cache;
    @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;fill(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)I"))
    private int ccd$measure(IFluidHandler handler,FluidStack fluid,IFluidHandler.FluidAction action,Operation<Integer> original){
        int filled=original.call(handler,fluid,action);
        if(action.execute()&&filled>0 && cache.values().stream().anyMatch(ref->{var pipe=ref.get();return pipe!=null&&pipe.blockEntity instanceof FlowMeterBlockEntity;}))for(var target:targets){
            var provider=target.getSecond().provideHandler();
            if(provider==null||provider.getCapability()!=handler)continue;
            var end=target.getFirst().getPos();
            var parents=new java.util.HashMap<BlockPos,BlockPos>();var queue=new java.util.ArrayDeque<BlockPos>();
            queue.add(start.getPos());parents.put(start.getPos(),start.getPos());
            while(!queue.isEmpty()&&parents.size()<=4096){
                var pos=queue.removeFirst();
                if(pos.equals(end)){
                    while(true){
                        if(world.getBlockEntity(pos) instanceof FlowMeterBlockEntity meter)meter.recordTransfer(filled);
                        var previous=parents.get(pos);if(previous.equals(pos))break;pos=previous;
                    }
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
}
