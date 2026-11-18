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
        Block.box(1, 0, 1, 15, 1, 15),
        Block.box(0, 1, 0, 16, 9, 16),
        Block.box(0, 9, 0, 16, 15, 2),
        Block.box(0, 9, 14, 16, 15, 16),
        Block.box(0, 9, 2, 2, 15, 14),
        Block.box(14, 9, 2, 16, 15, 14),
        Block.box(0, 15, 0, 16, 16, 2),
        Block.box(0, 15, 14, 16, 16, 16),
        Block.box(0, 15, 2, 2, 16, 14),
        Block.box(14, 15, 2, 16, 16, 14),
        Block.box(2, 9, 2, 14, 11, 3),
        Block.box(2, 9, 13, 14, 11, 14),
        Block.box(2, 9, 3, 3, 11, 13),
        Block.box(13, 9, 3, 14, 11, 13),
        Block.box(6.5, 16, 0, 9.5, 16.5, 2),
        Block.box(7, 16.5, 0.25, 9, 21.5, 1.75),
        Block.box(6, 19, 1.5, 10, 23, 3.5),
        Block.box(5.5, 18.5, 1.5, 10.5, 23.5, 2.5),
        Block.box(7, 20.5, 3, 9, 22.5, 6.5),
        Block.box(7, 20, 4.5, 9, 21, 6.5),
        Block.box(6.5, 19, 4, 9.5, 20, 7),
        Block.box(7, 17.5, 4.5, 9, 19, 6.5),
        Block.box(7.5, 22.5, 5, 8.5, 23, 6),
        Block.box(6, 23, 5, 10, 24, 6)), Direction.SOUTH);

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
