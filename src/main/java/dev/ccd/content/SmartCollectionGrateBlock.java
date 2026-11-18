package dev.ccd.content;

import com.mojang.serialization.MapCodec;
import dev.ccd.CopperDynamics;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Basic grate with event-driven redstone state; fluid and cauldron interaction remain inherited. */
public final class SmartCollectionGrateBlock extends CopperCollectionGrateBlock {
    public static final MapCodec<CauldronBlock> CODEC = simpleCodec(SmartCollectionGrateBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShaper SHAPES = VoxelShaper.forHorizontal(
        Shapes.or(SHAPE, box(5.5, 2, 14, 10.5, 7, 15.5)), Direction.SOUTH);
    public SmartCollectionGrateBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POWERED, false).setValue(FACING, Direction.SOUTH));
    }
    @Override public MapCodec<CauldronBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED, FACING);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
            .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    @Override public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return SHAPES.get(state.getValue(FACING));
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }
    @Override public BlockState getRotatedBlockState(BlockState state, Direction face) {
        return rotate(state, Rotation.CLOCKWISE_90);
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, block, from, moving);
        updateSignal(level, pos);
    }
    static void updateSignal(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SmartCollectionGrateBlock)) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) level.setBlockAndUpdate(pos, state.setValue(POWERED, powered));
    }
    @Override public BlockEntityType<? extends SmartCollectionGrateBlockEntity> getBlockEntityType() {
        return CopperDynamics.SMART_COLLECTION_GRATE_BE.get();
    }
}
