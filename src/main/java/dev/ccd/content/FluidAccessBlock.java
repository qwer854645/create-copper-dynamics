package dev.ccd.content;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** FACING points away from the attached container; bridges use both axial faces. */
public final class FluidAccessBlock extends DirectionalBlock implements IBE<FluidAccessBlockEntity>, IWrenchable {
    private final boolean bridge;
    private final MapCodec<FluidAccessBlock> codec;
    public FluidAccessBlock(Properties properties, boolean bridge) {
        super(properties);
        this.bridge = bridge;
        codec = simpleCodec(p -> new FluidAccessBlock(p, bridge));
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }
    public boolean isBridge() { return bridge; }
    @Override protected MapCodec<? extends DirectionalBlock> codec() { return codec; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getClickedFace()); }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    @Override public BlockState getRotatedBlockState(BlockState state, Direction face) {
        return state.setValue(FACING, state.getValue(FACING).getClockWise(face.getAxis()));
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        IBE.onRemove(state, level, pos, next);
        super.onRemove(state, level, pos, next, moving);
    }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide()) level.invalidateCapabilities(pos);
    }
    @Override public Class<FluidAccessBlockEntity> getBlockEntityClass() { return FluidAccessBlockEntity.class; }
    @Override public BlockEntityType<? extends FluidAccessBlockEntity> getBlockEntityType() {
        return bridge ? CopperDynamics.FLUID_BRIDGE_BE.get() : CopperDynamics.FLUID_ACCESS_PORT_BE.get();
    }
}
