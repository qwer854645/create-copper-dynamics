package dev.ccd.content;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.contraption.transformable.TransformableBlock;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;

public final class ThreeWayValveBlock extends DirectionalBlock
    implements IBE<ThreeWayValveBlockEntity>, IWrenchable, ProperWaterloggedBlock, TransformableBlock {
    public static final MapCodec<ThreeWayValveBlock> CODEC = simpleCodec(ThreeWayValveBlock::new);
    public static final IntegerProperty ROLL = IntegerProperty.create("roll", 0, 3);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public ThreeWayValveBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH).setValue(ROLL, 0)
            .setValue(POWERED, false).setValue(WATERLOGGED, false));
    }
    @Override protected MapCodec<? extends DirectionalBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ROLL, POWERED, WATERLOGGED);
    }
    /** FACING is straight port A; its opposite is common port C. */
    public static Direction branch(BlockState state) {
        Direction forward = state.getValue(FACING);
        Direction side = forward.getAxis().isVertical() ? Direction.NORTH : Direction.UP;
        for (int i = 0; i < state.getValue(ROLL); i++) side = side.getClockWise(forward.getAxis());
        return side;
    }
    public static boolean open(BlockState state, Direction side) {
        if (!(state.getBlock() instanceof ThreeWayValveBlock)) return false;
        Direction forward = state.getValue(FACING);
        return side == forward.getOpposite() || side == (state.getValue(POWERED) ? branch(state) : forward);
    }
    private static BlockState oriented(BlockState state, Direction forward, Direction side) {
        state = state.setValue(FACING, forward);
        for (int roll = 0; roll < 4; roll++) {
            BlockState candidate = state.setValue(ROLL, roll);
            if (branch(candidate) == side) return candidate;
        }
        throw new IllegalArgumentException("Three-way valve ports must be perpendicular");
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withWater(defaultBlockState().setValue(FACING, context.getClickedFace())
            .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos())), context);
    }
    @Override public BlockState getRotatedBlockState(BlockState state, Direction clickedFace) {
        Direction forward = state.getValue(FACING);
        if (clickedFace.getAxis() == forward.getAxis()) return state.cycle(ROLL);
        return oriented(state, forward.getClockWise(clickedFace.getAxis()), branch(state).getClockWise(clickedFace.getAxis()));
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return oriented(state, rotation.rotate(state.getValue(FACING)), rotation.rotate(branch(state)));
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        return oriented(state, mirror.mirror(state.getValue(FACING)), mirror.mirror(branch(state)));
    }
    @Override public BlockState transform(BlockState state, StructureTransform transform) {
        if (transform.mirror != null) state = mirror(state, transform.mirror);
        return oriented(state, transform.rotateFacing(state.getValue(FACING)), transform.rotateFacing(branch(state)));
    }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour,
        LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        updateWater(level, state, pos);
        return state;
    }
    @Override public FluidState getFluidState(BlockState state) { return fluidState(state); }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        updateSignal(level, pos);
        if (!level.isClientSide()) level.scheduleTick(pos, this, 1);
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block other,
        BlockPos neighbour, boolean moving) {
        updateSignal(level, pos);
        if (!level.isClientSide()) level.scheduleTick(pos, this, 1);
    }
    public static void updateSignal(Level level, BlockPos pos) {
        if (level.isClientSide() || !level.hasChunkAt(pos)) return;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ThreeWayValveBlock)) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (state.getValue(POWERED) != powered) level.setBlockAndUpdate(pos, state.setValue(POWERED, powered));
    }
    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        updateSignal(level, pos);
        refreshNetwork(level, pos);
    }
    /** Reset old connection networks before wipePressure removes inactive port entries.
     * Visit all neighbours so the branch closed by the new state is notified too. */
    public static void refreshNetwork(Level level, BlockPos pos) {
        FluidTransportBehaviour pipe = FluidPropagator.getPipe(level, pos);
        if (pipe != null) {
            if (pipe.interfaces != null) pipe.interfaces.values().forEach(connection -> connection.resetNetwork());
            pipe.wipePressure();
            FluidPropagator.propagateChangedPipe(level, pos, level.getBlockState(pos));
        }
        for (Direction side : Direction.values()) {
            BlockPos neighbour = pos.relative(side);
            if (!level.hasChunkAt(neighbour)) continue;
            if (level.getBlockEntity(neighbour) instanceof PumpBlockEntity pump) pump.updatePressureChange();
            else if (FluidPropagator.getPipe(level, neighbour) != null)
                FluidPropagator.propagateChangedPipe(level, neighbour, level.getBlockState(neighbour));
        }
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock())) {
            if (!level.isClientSide()) refreshNetwork(level, pos);
            IBE.onRemove(state, level, pos, next);
        }
        super.onRemove(state, level, pos, next, moving);
    }
    @Override public Class<ThreeWayValveBlockEntity> getBlockEntityClass() { return ThreeWayValveBlockEntity.class; }
    @Override public BlockEntityType<? extends ThreeWayValveBlockEntity> getBlockEntityType() {
        return CopperDynamics.THREE_WAY_VALVE_BE.get();
    }
}
