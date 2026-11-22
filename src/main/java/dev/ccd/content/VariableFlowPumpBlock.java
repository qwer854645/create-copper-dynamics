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
public class VariableFlowPumpBlock extends PumpBlock {
    public static final IntegerProperty ROLL=IntegerProperty.create("roll",0,3);
    public static final IntegerProperty SIGNAL=BlockStateProperties.POWER;
    public VariableFlowPumpBlock(Properties properties) {
        super(properties);registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH).setValue(SIGNAL,0).setValue(ROLL,0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
        super.createBlockStateDefinition(builder);builder.add(SIGNAL,ROLL);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return PumpPlacement.align(context,defaultBlockState().setValue(FACING,(this instanceof PortableFluidPumpBlock?context.getHorizontalDirection():context.getNearestLookingDirection()).getOpposite())
            .setValue(SIGNAL,context.getLevel().getBestNeighborSignal(context.getClickedPos()))
            .setValue(BlockStateProperties.WATERLOGGED,context.getLevel().getFluidState(context.getClickedPos()).is(net.minecraft.world.level.material.Fluids.WATER)));
    }
    @Override public Direction.Axis getRotationAxis(BlockState state) {
        return orientedSide(state,Direction.EAST).getAxis();
    }
    @Override public boolean hasShaftTowards(LevelReader level,BlockPos pos,BlockState state,Direction side) { return false; }
    @Override public boolean isSmallCog() { return true; }
    public static Direction.Axis gearContactAxis(BlockState state){
        return orientedSide(state,Direction.UP).getAxis();
    }
    private static Direction orientedSide(BlockState state,Direction side){
        for(int i=0;i<state.getValue(ROLL);i++)side=side.getClockWise(Direction.Axis.Z);
        var front=state.getValue(FACING);
        if(front.getAxis().isVertical())return front==Direction.UP?side.getCounterClockWise(Direction.Axis.X):side.getClockWise(Direction.Axis.X);
        int turns=switch(front){case EAST->1;case SOUTH->2;case WEST->3;default->0;};
        for(int i=0;i<turns;i++)side=side.getClockWise(Direction.Axis.Y);
        return side;
    }
    @Override public BlockState getRotatedBlockState(BlockState state,Direction clickedFace){
        var facing=state.getValue(FACING);
        if(this instanceof PortableFluidPumpBlock)return state.setValue(FACING,facing.getClockWise());
        return facing.getAxis()==clickedFace.getAxis()?state.cycle(ROLL):state.setValue(FACING,facing.getClockWise(clickedFace.getAxis()));
    }
    @Override public BlockEntityType<? extends PumpBlockEntity> getBlockEntityType() { return CopperDynamics.VARIABLE_FLOW_PUMP_BE.get(); }
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){
        super.onPlace(state,level,pos,old,moving);
        if(!level.isClientSide()&&old.getBlock()==this&&(old.getValue(FACING)!=state.getValue(FACING)||old.getValue(ROLL)!=state.getValue(ROLL))){
            for(var side:new Direction[]{old.getValue(FACING),old.getValue(FACING).getOpposite()}){
                var neighbor=pos.relative(side);
                com.simibubi.create.content.fluids.FluidPropagator.propagateChangedPipe(level,neighbor,level.getBlockState(neighbor));
            }
            if(level.getBlockEntity(pos) instanceof VariableFlowPumpBlockEntity pump)pump.updatePressureChange();
        }
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving) {
        super.neighborChanged(state,level,pos,block,from,moving);
        if(level.isClientSide())return;
        int signal=redstoneSignal(level,pos);
        if(signal!=state.getValue(SIGNAL)) {
            level.setBlockAndUpdate(pos,state.setValue(SIGNAL,signal));
            if(level.getBlockEntity(pos) instanceof VariableFlowPumpBlockEntity pump)pump.updatePressureChange();
        }
    }
    protected int redstoneSignal(Level level,BlockPos pos){return level.getBestNeighborSignal(pos);}
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.block();
    }
}
