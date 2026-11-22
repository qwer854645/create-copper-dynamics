package dev.ccd.content;

import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;

public final class RedstoneFluidPumpBlock extends PumpBlock {
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty ROLL=net.minecraft.world.level.block.state.properties.IntegerProperty.create("roll",0,3);
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty POWERED=BlockStateProperties.POWERED;
    public RedstoneFluidPumpBlock(Properties properties) { super(properties);registerDefaultState(defaultBlockState().setValue(POWERED,false).setValue(ROLL,0)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { super.createBlockStateDefinition(builder);builder.add(POWERED,ROLL); }
    @Override public BlockEntityType<? extends PumpBlockEntity> getBlockEntityType() { return CopperDynamics.REDSTONE_FLUID_PUMP_BE.get(); }
    @Override public Direction.Axis getRotationAxis(BlockState state) { return PumpOrientation.side(state,ROLL,Direction.EAST).getAxis(); }
    @Override public boolean hasShaftTowards(LevelReader level,BlockPos pos,BlockState state,Direction side) { return side.getAxis()==getRotationAxis(state); }
    @Override public boolean isSmallCog() { return false; }
    @Override public BlockState getRotatedBlockState(BlockState state,Direction clickedFace) {
        return PumpOrientation.next(state,ROLL);
    }
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving) {
        super.onPlace(state,level,pos,old,moving);
        if(!level.isClientSide()&&old.getBlock()==this&&(old.getValue(FACING)!=state.getValue(FACING)||old.getValue(ROLL)!=state.getValue(ROLL))) {
            for(Direction side:new Direction[]{old.getValue(FACING),old.getValue(FACING).getOpposite()}) {
                var neighbor=pos.relative(side);
                com.simibubi.create.content.fluids.FluidPropagator.propagateChangedPipe(level,neighbor,level.getBlockState(neighbor));
            }
            if(level.getBlockEntity(pos) instanceof RedstoneFluidPumpBlockEntity pump)pump.updatePressureChange();
        }
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return super.getStateForPlacement(context).setValue(POWERED,context.getLevel().hasNeighborSignal(context.getClickedPos())); }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving) {
        super.neighborChanged(state,level,pos,block,from,moving);
        if(level.isClientSide()) return;
        boolean powered=level.hasNeighborSignal(pos);
        if(powered!=state.getValue(POWERED)) {
            level.setBlockAndUpdate(pos,state.setValue(POWERED,powered));
            if(level.getBlockEntity(pos) instanceof RedstoneFluidPumpBlockEntity pump) pump.updatePressureChange();
        }
    }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context) { return net.minecraft.world.phys.shapes.Shapes.block(); }
}
