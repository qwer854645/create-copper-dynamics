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
    @Override public Direction.Axis getRotationAxis(BlockState state){return state.getValue(FACING).getAxis()==Direction.Axis.X?Direction.Axis.Z:Direction.Axis.X;}
    @Override public boolean hasShaftTowards(LevelReader level,BlockPos pos,BlockState state,Direction side){return side.getAxis()==getRotationAxis(state);}
    @Override public boolean isSmallCog(){return false;}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite()).setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,context.getLevel().getFluidState(context.getClickedPos()).is(net.minecraft.world.level.material.Fluids.WATER));}
    @Override public BlockState getRotatedBlockState(BlockState state,Direction side){return state.setValue(FACING,state.getValue(FACING).getClockWise());}
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return net.minecraft.world.phys.shapes.Shapes.block();}
}
