package dev.ccd.content;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import dev.ccd.CopperDynamics;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class BufferPumpPlacementHelper implements IPlacementHelper {
    @Override public java.util.function.Predicate<ItemStack> getItemPredicate(){return stack->stack.is(CopperDynamics.BUFFER_PUMP_ITEM.get());}
    @Override public java.util.function.Predicate<BlockState> getStatePredicate(){return ICogWheel::isSmallCog;}
    @Override public PlacementOffset getOffset(Player player,Level level,BlockState state,BlockPos pos,BlockHitResult hit){
        var axis=((IRotate)state.getBlock()).getRotationAxis(state);
        if(hit.getDirection().getAxis()==axis)return PlacementOffset.fail();
        for(var side:IPlacementHelper.orderedByDistanceExceptAxis(pos,hit.getLocation(),axis)){
            // The frequency pump exposes its gear only above and below.
            if(state.getBlock() instanceof ReciprocatingFluidPumpBlock && side.getAxis()!=Direction.Axis.Y)continue;
            var target=pos.relative(side);
            if(!level.getBlockState(target).canBeReplaced())continue;
            var facing=state.getBlock() instanceof BufferPumpBlock?state.getValue(BufferPumpBlock.FACING):Direction.fromAxisAndDirection(axis,Direction.AxisDirection.POSITIVE);
            var ghost=CopperDynamics.BUFFER_PUMP.get().defaultBlockState().setValue(BufferPumpBlock.FACING,facing);
            if(!ghost.canSurvive(level,target))continue;
            return PlacementOffset.success(target,placed->placed.setValue(BufferPumpBlock.FACING,facing)).withGhostState(ghost);
        }
        return PlacementOffset.fail();
    }
}
