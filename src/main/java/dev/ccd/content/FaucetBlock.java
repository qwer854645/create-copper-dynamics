package dev.ccd.content;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import dev.ccd.CopperDynamics;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FaucetBlock extends HorizontalDirectionalBlock
    implements IBE<FaucetBlockEntity>, IWrenchable, ProperWaterloggedBlock {
    public static final MapCodec<FaucetBlock> CODEC = simpleCodec(FaucetBlock::new);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape BODY = Shapes.or(
        Block.box(3, 3, 0, 13, 13, 4),
        Block.box(4, 4, 4, 12, 14, 12),
        Block.box(6, 1, 6, 10, 4, 10)).optimize();
    protected static final VoxelShape CLOSED_SHAPE = BODY;
    protected static final VoxelShape OPEN_SHAPE = BODY;
    private static final VoxelShaper CLOSED_SHAPES = VoxelShaper.forHorizontal(CLOSED_SHAPE, Direction.SOUTH);
    private static final VoxelShaper OPEN_SHAPES = VoxelShaper.forHorizontal(OPEN_SHAPE, Direction.SOUTH);
    public FaucetBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH)
            .setValue(OPEN, false).setValue(POWERED, false).setValue(WATERLOGGED, false));
    }
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    protected VoxelShaper modelShaper(boolean open) { return open ? OPEN_SHAPES : CLOSED_SHAPES; }
    private VoxelShape modelShape(BlockState state) {
        boolean open = state.getValue(OPEN) && !state.getValue(POWERED);
        return modelShaper(open).get(state.getValue(FACING));
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return modelShape(state);
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return modelShape(state);
    }
    @Override public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return modelShape(state);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, POWERED, WATERLOGGED);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (face.getAxis().isVertical()) face = context.getHorizontalDirection().getOpposite();
        return withWater(defaultBlockState().setValue(FACING, face)
            .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos())), context);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
        Player player, BlockHitResult hit) {
        // Like the native lever, also allow the normal item-use fallback to operate the handle.
        if (!level.isClientSide()) level.setBlockAndUpdate(pos, state.cycle(OPEN));
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
    @Override public BlockState getRotatedBlockState(BlockState state, Direction face) {
        return state.setValue(FACING, state.getValue(FACING).getClockWise());
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbour,
        LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        updateWater(level, state, pos);
        return state;
    }
    @Override public FluidState getFluidState(BlockState state) { return fluidState(state); }
    public static void updateSignal(Level level, BlockPos pos) {
        if (level.isClientSide() || !level.hasChunkAt(pos)) return;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FaucetBlock)) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (state.getValue(POWERED) != powered) level.setBlockAndUpdate(pos, state.setValue(POWERED, powered));
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block other,
        BlockPos neighbour, boolean moving) { updateSignal(level, pos); }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        updateSignal(level, pos);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        IBE.onRemove(state, level, pos, next);
        super.onRemove(state, level, pos, next, moving);
    }
    @Override public Class<FaucetBlockEntity> getBlockEntityClass() { return FaucetBlockEntity.class; }
    @Override public BlockEntityType<? extends FaucetBlockEntity> getBlockEntityType() { return CopperDynamics.FAUCET_BE.get(); }
}
