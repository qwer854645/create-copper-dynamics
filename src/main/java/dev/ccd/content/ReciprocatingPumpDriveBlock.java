package dev.ccd.content;

import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/** Upper shaft bearing. No item or independent drop: belongs to the lower pump. */
public final class ReciprocatingPumpDriveBlock extends RotatedPillarKineticBlock implements IBE<ReciprocatingPumpDriveBlockEntity> {
    public ReciprocatingPumpDriveBlock(Properties properties){super(properties);registerDefaultState(defaultBlockState().setValue(AXIS,Direction.Axis.X));}
    @Override public Direction.Axis getRotationAxis(BlockState state){return state.getValue(AXIS);}
    @Override public boolean hasShaftTowards(LevelReader level,BlockPos pos,BlockState state,Direction side){return side.getAxis()==getRotationAxis(state);}
    @Override public Class<ReciprocatingPumpDriveBlockEntity> getBlockEntityClass(){return ReciprocatingPumpDriveBlockEntity.class;}
    @Override public net.minecraft.world.level.block.entity.BlockEntityType<? extends ReciprocatingPumpDriveBlockEntity> getBlockEntityType(){return CopperDynamics.RECIPROCATING_DRIVE_BE.get();}
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&!level.isClientSide()&&level.getBlockState(pos.below()).is(CopperDynamics.RECIPROCATING_PUMP.get()))level.destroyBlock(pos.below(),true);
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,net.minecraft.world.level.block.Block block,BlockPos from,boolean moving){
        super.neighborChanged(state,level,pos,block,from,moving);
        if(!level.isClientSide()&&level.getBlockState(pos.below()).getBlock() instanceof TallReciprocatingPumpBlock pump)
            pump.neighborChanged(level.getBlockState(pos.below()),level,pos.below(),block,from,moving);
    }
    @Override public net.minecraft.world.InteractionResult onWrenched(BlockState state,net.minecraft.world.item.context.UseOnContext context){
        // The bearing cannot be rotated independently from its pump body.
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
    @Override public net.minecraft.world.InteractionResult onSneakWrenched(BlockState state,net.minecraft.world.item.context.UseOnContext context){
        var level=context.getLevel();var bottom=context.getClickedPos().below();
        if(!level.isClientSide()&&level.getBlockState(bottom).is(CopperDynamics.RECIPROCATING_PUMP.get())){
            var player=context.getPlayer();
            if(player!=null&&!player.isCreative()){
                var stack=new net.minecraft.world.item.ItemStack(CopperDynamics.RECIPROCATING_PUMP_ITEM.get());
                if(!player.getInventory().add(stack))player.drop(stack,false);
            }
            level.destroyBlock(bottom,false);
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){
        return net.minecraft.world.phys.shapes.Shapes.or(box(1,0,5,3,12,11),box(13,0,5,15,12,11),box(1,9,5,15,13,11));
    }
}
