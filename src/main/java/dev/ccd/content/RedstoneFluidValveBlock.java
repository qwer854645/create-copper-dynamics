package dev.ccd.content;

import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlock;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlockEntity;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Native valve geometry and pipe behaviour, operated by a redstone actuator instead of shafts. */
public final class RedstoneFluidValveBlock extends FluidValveBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private final java.util.Map<BlockState,net.minecraft.world.phys.shapes.VoxelShape> outlines = new java.util.HashMap<>();

    public RedstoneFluidValveBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POWERED, false).setValue(ENABLED, true));
        for(var state:getStateDefinition().getPossibleStates()) {
            var nativeShape=super.getShape(state,net.minecraft.world.level.EmptyBlockGetter.INSTANCE,BlockPos.ZERO,
                net.minecraft.world.phys.shapes.CollisionContext.empty());
            outlines.put(state,net.minecraft.world.phys.shapes.Shapes.create(nativeShape.bounds()));
        }
    }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,
        BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context) { return outlines.get(state); }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state,net.minecraft.world.level.BlockGetter level,
        BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context) { return getShape(state,level,pos,context); }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getInteractionShape(BlockState state,net.minecraft.world.level.BlockGetter level,
        BlockPos pos) { return outlines.get(state); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }
    @Override public BlockEntityType<? extends FluidValveBlockEntity> getBlockEntityType() {
        return CopperDynamics.REDSTONE_FLUID_VALVE_BE.get();
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean powered = context.getLevel().hasNeighborSignal(context.getClickedPos());
        return super.getStateForPlacement(context).setValue(POWERED, powered).setValue(ENABLED, !powered);
    }
    @Override public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return false;
    }
    @Override protected boolean prefersConnectionTo(LevelReader world, BlockPos pos, Direction face, boolean shaftAxis) {
        return !shaftAxis && super.prefersConnectionTo(world, pos, face, false);
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block other,
        BlockPos neighbour, boolean moving) {
        super.neighborChanged(state, level, pos, other, neighbour, moving);
        updateSignal(level, pos);
    }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        updateSignal(level, pos);
    }
    public static void updateSignal(Level level, BlockPos pos) {
        if (level.isClientSide() || !level.hasChunkAt(pos)) return;
        BlockState current = level.getBlockState(pos);
        if (!(current.getBlock() instanceof RedstoneFluidValveBlock)) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (current.getValue(POWERED) != powered)
            level.setBlockAndUpdate(pos, current.setValue(POWERED, powered));
    }
}
