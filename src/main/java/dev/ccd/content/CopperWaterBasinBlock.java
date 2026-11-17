package dev.ccd.content;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Native creative-tank interaction, with a permanently water-filled, single-block entity. */
public final class CopperWaterBasinBlock extends FluidTankBlock {
    public static final MapCodec<CopperWaterBasinBlock> CODEC = simpleCodec(CopperWaterBasinBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 4, 16),
        Block.box(0, 4, 0, 16, 14, 1), Block.box(0, 4, 15, 16, 14, 16),
        Block.box(0, 4, 1, 1, 14, 15), Block.box(15, 4, 1, 16, 14, 15));

    public CopperWaterBasinBlock(Properties properties) { super(properties, true); }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    @Override public BlockEntityType<? extends CopperWaterBasinBlockEntity> getBlockEntityType() {
        return CopperDynamics.COPPER_WATER_BASIN_BE.get();
    }
}
