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
        Block.box(1, 1, 1, 15, 9, 15),
        Block.box(0, 1, 0, 16, 15, 3),
        Block.box(0, 1, 14, 16, 15, 16),
        Block.box(0, 1, 3, 2, 15, 14),
        Block.box(14, 1, 3, 16, 15, 14),
        Block.box(0, 15, 0, 16, 16, 3),
        Block.box(0, 15, 14, 16, 16, 16),
        Block.box(0, 15, 3, 2, 16, 14),
        Block.box(14, 15, 3, 16, 16, 14),
        Block.box(6.5, 16, 0.5, 9.5, 17, 3.5),
        Block.box(7.5, 17, 1.5, 8.5, 21, 2.5),
        Block.box(7.5, 20, 2.5, 8.5, 21, 7),
        Block.box(7.5, 18.5, 6, 8.5, 20, 7),
        Block.box(9, 17, 1, 11, 17.75, 2),
        Block.box(6, 7, 15.75, 6.75, 10, 16.25),
        Block.box(9.25, 7, 15.75, 10, 10, 16.25),
        Block.box(2, 9, 3, 14, 11, 4),
        Block.box(2, 9, 13, 14, 11, 14),
        Block.box(2, 9, 4, 3, 11, 13),
        Block.box(13, 9, 4, 14, 11, 13)), Direction.SOUTH);

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
