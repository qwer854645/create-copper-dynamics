package dev.ccd.content;

import com.mojang.serialization.MapCodec;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Basic grate with event-driven redstone state; fluid and cauldron interaction remain inherited. */
public final class SmartCollectionGrateBlock extends CopperCollectionGrateBlock {
    public static final MapCodec<CauldronBlock> CODEC = simpleCodec(SmartCollectionGrateBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public SmartCollectionGrateBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }
    @Override public MapCodec<CauldronBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
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
