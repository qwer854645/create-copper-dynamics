package dev.ccd.content;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.logistics.itemHatch.ItemHatchBlockEntity;
import dev.ccd.CopperDynamics;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SmartFluidHatchBlock extends FluidHatchBlock {
    public static final MapCodec<SmartFluidHatchBlock> CODEC = simpleCodec(SmartFluidHatchBlock::new);
    private static final VoxelShaper SHAPE = VoxelShaper.forHorizontal(Shapes.or(
        Block.box(1, 0, 0, 15, 16, 2), Block.box(3, 0, 2, 13, 10, 4),
        Block.box(5, 10, 2, 11, 16, 3)), Direction.SOUTH);
    public SmartFluidHatchBlock(Properties properties) { super(properties); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE.get(state.getValue(FACING).getOpposite());
    }
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    @Override public BlockEntityType<? extends ItemHatchBlockEntity> getBlockEntityType() {
        return CopperDynamics.SMART_FLUID_HATCH_BE.get();
    }
}
