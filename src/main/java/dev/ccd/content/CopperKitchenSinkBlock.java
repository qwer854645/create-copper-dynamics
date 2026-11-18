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
        Block.box(0, 0, 0, 16, 2, 16), Block.box(0, 2, 0, 16, 8, 3),
        Block.box(0, 2, 14, 16, 8, 16), Block.box(0, 2, 3, 2, 8, 14),
        Block.box(14, 2, 3, 16, 8, 14), Block.box(6, 8, 0, 10, 9, 4),
        Block.box(7, 9, 1, 9, 15, 3), Block.box(7, 13, 3, 9, 15, 7),
        Block.box(7, 11, 6, 9, 13, 8)), Direction.SOUTH);

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
