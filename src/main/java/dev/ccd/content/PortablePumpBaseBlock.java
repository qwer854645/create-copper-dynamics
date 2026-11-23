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
public class PortablePumpBaseBlock extends PumpBlock implements com.simibubi.create.api.contraption.transformable.TransformableBlock {
    public static final IntegerProperty ROLL=IntegerProperty.create("roll",0,3);
    public static final IntegerProperty SIGNAL=BlockStateProperties.POWER;
    public PortablePumpBaseBlock(Properties properties) {
        super(properties);registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH).setValue(SIGNAL,0).setValue(ROLL,0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
        super.createBlockStateDefinition(builder);builder.add(SIGNAL,ROLL);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return PumpPlacement.align(context,defaultBlockState().setValue(FACING,context.getNearestLookingDirection().getOpposite())
            .setValue(SIGNAL,context.getLevel().getBestNeighborSignal(context.getClickedPos()))
            .setValue(BlockStateProperties.WATERLOGGED,context.getLevel().getFluidState(context.getClickedPos()).is(net.minecraft.world.level.material.Fluids.WATER)));
    }
    @Override public Direction.Axis getRotationAxis(BlockState state) {
        return orientedSide(state,Direction.EAST).getAxis();
    }
    @Override public boolean hasShaftTowards(LevelReader level,BlockPos pos,BlockState state,Direction side) { return false; }
    @Override public boolean isSmallCog() { return false; }
    public static Direction.Axis gearContactAxis(BlockState state){
        return orientedSide(state,Direction.UP).getAxis();
    }
    private static Direction orientedSide(BlockState state,Direction side){
        return PumpOrientation.side(state,ROLL,side);
    }
    @Override public BlockState getRotatedBlockState(BlockState state,Direction clickedFace){
        var facing=state.getValue(FACING);
        return PumpOrientation.rotate(state,ROLL,clickedFace);
    }
    @Override public BlockEntityType<? extends PumpBlockEntity> getBlockEntityType() { return CopperDynamics.PORTABLE_PUMP_BE.get(); }
    @Override public BlockState rotate(BlockState state,net.minecraft.world.level.block.Rotation rotation){return PumpOrientation.oriented(state,ROLL,rotation.rotate(state.getValue(FACING)),rotation.rotate(PumpOrientation.side(state,ROLL,Direction.EAST)));}
    @Override public BlockState mirror(BlockState state,net.minecraft.world.level.block.Mirror mirror){return PumpOrientation.oriented(state,ROLL,mirror.mirror(state.getValue(FACING)),mirror.mirror(PumpOrientation.side(state,ROLL,Direction.EAST)));}
    @Override public BlockState transform(BlockState state,com.simibubi.create.content.contraptions.StructureTransform transform){return PumpOrientation.transform(state,ROLL,transform);}
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){
        super.onPlace(state,level,pos,old,moving);
        if(!level.isClientSide()&&old.getBlock()==this&&(old.getValue(FACING)!=state.getValue(FACING)||old.getValue(ROLL)!=state.getValue(ROLL))){
            for(var side:new Direction[]{old.getValue(FACING),old.getValue(FACING).getOpposite()}){
                var neighbor=pos.relative(side);
                com.simibubi.create.content.fluids.FluidPropagator.propagateChangedPipe(level,neighbor,level.getBlockState(neighbor));
            }
            if(level.getBlockEntity(pos) instanceof PortablePumpBaseBlockEntity pump)pump.updatePressureChange();
        }
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving) {
        super.neighborChanged(state,level,pos,block,from,moving);
        if(level.isClientSide())return;
        int signal=redstoneSignal(level,pos);
        if(signal!=state.getValue(SIGNAL)) {
            level.setBlockAndUpdate(pos,state.setValue(SIGNAL,signal));
            if(level.getBlockEntity(pos) instanceof PortablePumpBaseBlockEntity pump)pump.updatePressureChange();
        }
    }
    protected int redstoneSignal(Level level,BlockPos pos){return level.getBestNeighborSignal(pos);}
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.block();
    }
}
