package dev.ccd.content;

import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Copper flow-control pump with straight horizontal ports and a transverse gear shaft. */
public class ReciprocatingFluidPumpBlock extends PumpBlock {
    public static final IntegerProperty SIGNAL=BlockStateProperties.POWER;
    public ReciprocatingFluidPumpBlock(Properties properties) {
        super(properties);registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH).setValue(SIGNAL,0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
        super.createBlockStateDefinition(builder);builder.add(SIGNAL);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return PumpPlacement.align(context,defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite())
            .setValue(SIGNAL,context.getLevel().getBestNeighborSignal(context.getClickedPos()))
            .setValue(BlockStateProperties.WATERLOGGED,context.getLevel().getFluidState(context.getClickedPos()).is(net.minecraft.world.level.material.Fluids.WATER)));
    }
    @Override public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis()==Direction.Axis.X?Direction.Axis.Z:Direction.Axis.X;
    }
    @Override public boolean hasShaftTowards(LevelReader level,BlockPos pos,BlockState state,Direction side) { return false; }
    @Override public boolean isSmallCog() { return true; }
    @Override public BlockEntityType<? extends PumpBlockEntity> getBlockEntityType() { return CopperDynamics.RECIPROCATING_PUMP_BE.get(); }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving) {
        super.neighborChanged(state,level,pos,block,from,moving);
        if(level.isClientSide())return;
        int signal=redstoneSignal(level,pos);
        if(signal!=state.getValue(SIGNAL)) {
            level.setBlockAndUpdate(pos,state.setValue(SIGNAL,signal));
            if(level.getBlockEntity(pos) instanceof ReciprocatingFluidPumpBlockEntity pump)pump.updatePressureChange();
        }
    }
    protected int redstoneSignal(Level level,BlockPos pos){return level.getBestNeighborSignal(pos);}
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.block();
    }
}
