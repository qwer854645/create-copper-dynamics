package dev.ccd.content;

import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.context.BlockPlaceContext;

/** Lower fluid body; the upper block alone belongs to the kinetic network. */
public final class TallReciprocatingPumpBlock extends ReciprocatingFluidPumpBlock {
    public TallReciprocatingPumpBlock(Properties properties) { super(properties); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        if(context.getClickedPos().getY()>=context.getLevel().getMaxBuildHeight()-1
            ||!context.getLevel().getBlockState(context.getClickedPos().above()).canBeReplaced(context))return null;
        return super.getStateForPlacement(context);
    }
    @Override public boolean hasShaftTowards(LevelReader level,BlockPos pos,BlockState state,Direction side){return false;}
    @Override protected int redstoneSignal(Level level,BlockPos pos){return Math.max(level.getBestNeighborSignal(pos),level.getBestNeighborSignal(pos.above()));}
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity placer,net.minecraft.world.item.ItemStack stack){
        super.setPlacedBy(level,pos,state,placer,stack);
        level.setBlockAndUpdate(pos.above(),CopperDynamics.RECIPROCATING_DRIVE.get().defaultBlockState()
            .setValue(ReciprocatingPumpDriveBlock.AXIS,getRotationAxis(state)));
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&!level.isClientSide()&&level.getBlockState(pos.above()).is(CopperDynamics.RECIPROCATING_DRIVE.get()))
            level.destroyBlock(pos.above(),false);
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){
        return net.minecraft.world.phys.shapes.Shapes.or(box(2,2,2,14,12,14),box(6,12,6,10,16,10));
    }
}
