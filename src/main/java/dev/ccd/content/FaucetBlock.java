package dev.ccd.content;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
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

public class FaucetBlock extends HorizontalDirectionalBlock
    implements IBE<FaucetBlockEntity>, IWrenchable, ProperWaterloggedBlock {
    public static final MapCodec<FaucetBlock> CODEC = simpleCodec(FaucetBlock::new);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public FaucetBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH)
            .setValue(OPEN, false).setValue(POWERED, false).setValue(WATERLOGGED, false));
    }
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
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
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
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
