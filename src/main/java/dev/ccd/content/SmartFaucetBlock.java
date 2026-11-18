package dev.ccd.content;

import com.mojang.serialization.MapCodec;
import dev.ccd.CopperDynamics;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SmartFaucetBlock extends FaucetBlock {
    public static final MapCodec<SmartFaucetBlock> CODEC = simpleCodec(SmartFaucetBlock::new);
    private static final VoxelShape FILTER = Block.box(4.9, 4.9, 9.8, 11.1, 11.1, 11.2);
    private static final VoxelShaper CLOSED_SHAPES = VoxelShaper.forHorizontal(Shapes.or(CLOSED_SHAPE, FILTER), Direction.SOUTH);
    private static final VoxelShaper OPEN_SHAPES = VoxelShaper.forHorizontal(Shapes.or(OPEN_SHAPE, FILTER), Direction.SOUTH);
    public SmartFaucetBlock(Properties properties) { super(properties); }
    @Override protected VoxelShaper modelShaper(boolean open) { return open ? OPEN_SHAPES : CLOSED_SHAPES; }
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    @Override public BlockEntityType<? extends FaucetBlockEntity> getBlockEntityType() {
        return CopperDynamics.SMART_FAUCET_BE.get();
    }
}
