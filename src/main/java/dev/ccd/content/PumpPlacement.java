package dev.ccd.content;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

/** Align physical mouths, independent of pressure, selected fluid or power state. */
public final class PumpPlacement {
    public static boolean isCcdPump(BlockState state) {
        return state.getBlock() instanceof BoosterPumpBlock || state.getBlock() instanceof TurningBoosterPumpBlock
            || state.getBlock() instanceof BidirectionalFluidPumpBlock || state.getBlock() instanceof RedstoneFluidPumpBlock
            || state.getBlock() instanceof VariableFlowPumpBlock || state.getBlock() instanceof BufferPumpBlock;
    }
    public static boolean mouth(BlockState state,Direction side) {
        if(state.getBlock() instanceof ThreeWayValveBlock)
            return side.getAxis()==state.getValue(ThreeWayValveBlock.FACING).getAxis() || side==ThreeWayValveBlock.branch(state);
        if(state.getBlock() instanceof FluidTransmitterBlock)
            return side==state.getValue(FluidTransmitterBlock.FACING).getOpposite();
        if(state.getBlock() instanceof TurningBoosterPumpBlock)return TurningBoosterPumpBlock.open(state,side);
        if(state.getBlock() instanceof BufferPumpBlock || state.getBlock() instanceof CreativeDeliveryTankBlock)
            return side==state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        return state.getBlock() instanceof PumpBlock && side.getAxis()==state.getValue(PumpBlock.FACING).getAxis();
    }
    public static boolean connects(BlockPlaceContext context,Direction side) {
        var pos=context.getClickedPos().relative(side);var world=context.getLevel();
        if(!world.hasChunkAt(pos))return false;
        var state=world.getBlockState(pos);
        if(state.getBlock() instanceof FlowMeterBlock)return FluidPipeBlock.isOpenAt(state,side.getOpposite());
        if(state.getBlock() instanceof PumpBlock || state.getBlock() instanceof CreativeDeliveryTankBlock
            || state.getBlock() instanceof ThreeWayValveBlock || state.getBlock() instanceof FluidTransmitterBlock)
            return mouth(state,side.getOpposite());
        // A native pipe may grow a new arm toward this placement.
        if(FluidPipeBlock.isPipe(state))return true;
        var pipe=FluidPropagator.getPipe(world,pos);
        if(pipe!=null)return pipe.canHaveFlowToward(state,side.getOpposite());
        return world.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,pos,side.getOpposite())!=null;
    }
    public static Direction preferredConnection(BlockPlaceContext context){
        var attached=context.getClickedFace().getOpposite();
        if(connects(context,attached))return attached;
        for(var side:context.getNearestLookingDirections())if(connects(context,side))return side;
        return null;
    }
    public static BlockState align(BlockPlaceContext context,BlockState fallback) {
        if(fallback==null||!isCcdPump(fallback))return fallback;
        Direction attached=context.getClickedFace().getOpposite();
        boolean horizontal=fallback.getBlock() instanceof PortableFluidPumpBlock;
        Direction preferred=null;
        if((!horizontal||attached.getAxis().isHorizontal())&&connects(context,attached))preferred=attached;
        if(preferred==null)for(var side:context.getPlayer()==null?Direction.values():Direction.orderedByNearest(context.getPlayer())) {
            if((!horizontal||side.getAxis().isHorizontal())&&connects(context,side)){preferred=side;break;}
        }
        if(preferred==null)return fallback;
        if(fallback.getBlock() instanceof BufferPumpBlock)return fallback.setValue(PumpBlock.FACING,preferred);
        if(fallback.getBlock() instanceof TurningBoosterPumpBlock) {
            // Preserve the clicked connection as the inlet; choose a perpendicular outlet.
            BlockState best=null;int bestScore=-1;
            for(var front:Direction.values())if(front.getAxis()!=preferred.getAxis())for(int roll=0;roll<4;roll++) {
                var candidate=fallback.setValue(PumpBlock.FACING,front).setValue(TurningBoosterPumpBlock.ROLL,roll);
                if(TurningBoosterPumpBlock.branch(candidate)!=preferred)continue;
                int score=(connects(context,front)?10:0)+(front==fallback.getValue(PumpBlock.FACING)?2:0);
                if(score>bestScore){best=candidate;bestScore=score;}
            }
            return best==null?fallback:best;
        }
        var facing=fallback.getValue(PumpBlock.FACING);
        return facing.getAxis()==preferred.getAxis()?fallback:fallback.setValue(PumpBlock.FACING,preferred.getOpposite());
    }
    private PumpPlacement(){}
}
