package dev.ccd.content;

import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public final class BufferPumpBlock extends PumpBlock {
    public BufferPumpBlock(Properties properties){super(properties);}
    @Override public net.minecraft.world.level.block.entity.BlockEntityType<? extends PumpBlockEntity> getBlockEntityType(){return CopperDynamics.BUFFER_PUMP_BE.get();}
    @Override public Direction.Axis getRotationAxis(BlockState state){return state.getValue(FACING).getAxis();}
    @Override public boolean hasShaftTowards(LevelReader level,BlockPos pos,BlockState state,Direction side){return false;}
    @Override public boolean isSmallCog(){return true;}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){return super.getStateForPlacement(context);}
    @Override public BlockState getRotatedBlockState(BlockState state,Direction side){
        Direction facing=state.getValue(FACING);
        Direction.Axis axis=side.getAxis()==facing.getAxis()
            ?(facing.getAxis()==Direction.Axis.Y?Direction.Axis.X:Direction.Axis.Y):side.getAxis();
        return state.setValue(FACING,facing.getClockWise(axis));
    }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
        if(stack.is(net.neoforged.neoforge.common.Tags.Items.TOOLS_WRENCH)){
            var context=new net.minecraft.world.item.context.UseOnContext(player,hand,hit);
            var result=player.isShiftKeyDown()?onSneakWrenched(state,context):onWrenched(state,context);
            return result.consumesAction()?net.minecraft.world.ItemInteractionResult.SUCCESS:net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if(player.isCreative()){
            var sample=FluidStockItem.sample(stack);
            if(!sample.isEmpty()&&level.getBlockEntity(pos) instanceof BufferPumpBlockEntity pump){
                int accepted=pump.buffer.fill(sample.copyWithAmount(BufferPumpBlockEntity.CAPACITY),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
                if(accepted==0)return net.minecraft.world.ItemInteractionResult.FAIL;
                if(!level.isClientSide())pump.buffer.fill(sample.copyWithAmount(accepted),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                return net.minecraft.world.ItemInteractionResult.SUCCESS;
            }
        }
        return super.useItemOn(stack,state,level,pos,player,hand,hit);
    }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return net.minecraft.world.phys.shapes.Shapes.block();}
}
