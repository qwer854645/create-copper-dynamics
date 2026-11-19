package dev.ccd.content;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Kitchen-shaped basin; all water and handheld interaction remain in the basin parent. */
public final class CopperKitchenSinkBlock extends CopperWaterBasinBlock {
    public static final MapCodec<CopperKitchenSinkBlock> CODEC = simpleCodec(CopperKitchenSinkBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShaper SHAPE = VoxelShaper.forHorizontal(Shapes.or(
        Block.box(0, 0, 0, 16, 9, 16),
        Block.box(0, 9, 0, 16, 16, 2),
        Block.box(0, 9, 14, 16, 16, 16),
        Block.box(0, 9, 2, 2, 16, 14),
        Block.box(14, 9, 2, 16, 16, 14),
        Block.box(6, 16, 0, 10, 22, 5)).optimize(), Direction.SOUTH);

    public CopperKitchenSinkBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH));
    }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE.get(state.getValue(FACING));
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    @Override public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return SHAPE.get(state.getValue(FACING));
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }
    @Override public BlockState getRotatedBlockState(BlockState state, Direction face) {
        return rotate(state, Rotation.CLOCKWISE_90);
    }
    @Override public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        // Native tank wrenching toggles windows, which this always-open basin does not have.
        if (!context.getLevel().isClientSide()) {
            context.getLevel().setBlockAndUpdate(context.getClickedPos(), getRotatedBlockState(state, context.getClickedFace()));
            IWrenchable.playRotateSound(context.getLevel(), context.getClickedPos());
        }
        return InteractionResult.SUCCESS;
    }
}
