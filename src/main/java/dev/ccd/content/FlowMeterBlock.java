package dev.ccd.content;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Native pipe transport with two fixed, opposite mouths. FACING is the dial face. */
public final class FlowMeterBlock extends FluidPipeBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final MapCodec<FlowMeterBlock> CODEC=simpleCodec(FlowMeterBlock::new);
    public FlowMeterBlock(Properties properties){super(properties);registerDefaultState(getAxisState(Direction.Axis.Z).setValue(FACING,Direction.EAST));}
    @Override protected MapCodec<? extends net.minecraft.world.level.block.PipeBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){super.createBlockStateDefinition(builder);builder.add(FACING);}
    @Override public BlockEntityType<? extends FluidPipeBlockEntity> getBlockEntityType(){return CopperDynamics.FLOW_METER_BE.get();}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        var connection=PumpPlacement.preferredConnection(context);
        var axis=connection==null?context.getNearestLookingDirection().getAxis():connection.getAxis();
        var face=context.getHorizontalDirection().getOpposite();
        if(face.getAxis()==axis)face=axis==Direction.Axis.X?Direction.NORTH:Direction.EAST;
        return getAxisState(axis).setValue(FACING,face).setValue(BlockStateProperties.WATERLOGGED,context.getLevel().getFluidState(context.getClickedPos()).is(net.minecraft.world.level.material.Fluids.WATER));
    }
    @Override public BlockState updateBlockState(BlockState state,Direction preferred,Direction ignore,BlockAndTintGetter world,BlockPos pos){return state;}
    @Override public InteractionResult onWrenched(BlockState state,UseOnContext context){
        var axis=com.simibubi.create.content.fluids.FluidPropagator.getStraightPipeAxis(state);
        var face=state.getValue(FACING).getOpposite();
        if(axis==Direction.Axis.Y)face=state.getValue(FACING).getClockWise();
        if(!context.getLevel().isClientSide())context.getLevel().setBlockAndUpdate(context.getClickedPos(),state.setValue(FACING,face));
        return InteractionResult.SUCCESS;
    }
    @Override public BlockState rotate(BlockState state,net.minecraft.world.level.block.Rotation rotation){return super.rotate(state,rotation).setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public BlockState mirror(BlockState state,net.minecraft.world.level.block.Mirror mirror){return super.mirror(state,mirror).setValue(FACING,mirror.mirror(state.getValue(FACING)));}
    @Override public VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter world,BlockPos pos,CollisionContext context){return Shapes.block();}
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;}
}
