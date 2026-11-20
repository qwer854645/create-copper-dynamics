package dev.ccd.content;

import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Straight fluid ports and one opposite shaft pair. */
public final class BidirectionalFluidPumpBlock extends PumpBlock implements com.simibubi.create.api.contraption.transformable.TransformableBlock {
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty ROLL =
        net.minecraft.world.level.block.state.properties.IntegerProperty.create("roll", 0, 3);
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty REVERSED =
        net.minecraft.world.level.block.state.properties.BooleanProperty.create("reversed");
    public BidirectionalFluidPumpBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(ROLL, 0).setValue(REVERSED, false));
    }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ROLL, REVERSED);
    }
    public static Direction shaftDirection(BlockState state) {
        Direction front = state.getValue(FACING);
        Direction shaft = front.getAxis().isVertical() ? Direction.SOUTH
            : front.getAxis() == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
        for (int i = 0; i < state.getValue(ROLL); i++) shaft = shaft.getClockWise(front.getAxis());
        return shaft;
    }
    private static BlockState oriented(BlockState state, Direction front, Direction shaft) {
        state = state.setValue(FACING, front);
        for (int roll = 0; roll < 4; roll++) {
            var candidate = state.setValue(ROLL, roll);
            if (shaftDirection(candidate) == shaft) return candidate;
        }
        throw new IllegalArgumentException("Pump shaft must be perpendicular to fluid ports");
    }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return super.getStateForPlacement(context).setValue(ROLL, 0).setValue(REVERSED, false);
    }
    @Override public BlockState getRotatedBlockState(BlockState state, Direction clickedFace) {
        return oriented(state, state.getValue(FACING).getClockWise(clickedFace.getAxis()),
            shaftDirection(state).getClockWise(clickedFace.getAxis()));
    }
    @Override public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return oriented(state, rotation.rotate(state.getValue(FACING)), rotation.rotate(shaftDirection(state)));
    }
    @Override public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return oriented(state, mirror.mirror(state.getValue(FACING)), mirror.mirror(shaftDirection(state)));
    }
    @Override public BlockState transform(BlockState state, com.simibubi.create.content.contraptions.StructureTransform transform) {
        if (transform.mirror != null) state = mirror(state, transform.mirror);
        return oriented(state, transform.rotateFacing(state.getValue(FACING)), transform.rotateFacing(shaftDirection(state)));
    }
    @Override public void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide() && old.getBlock() == this && old.getValue(FACING) != state.getValue(FACING)) {
            for (Direction side : new Direction[]{old.getValue(FACING), old.getValue(FACING).getOpposite()}) {
                var neighbour = pos.relative(side);
                com.simibubi.create.content.fluids.FluidPropagator.propagateChangedPipe(level, neighbour, level.getBlockState(neighbour));
            }
            if (level.getBlockEntity(pos) instanceof BidirectionalFluidPumpBlockEntity pump) pump.updatePressureChange();
        }
    }
    @Override public BlockEntityType<? extends PumpBlockEntity> getBlockEntityType() {
        return CopperDynamics.BIDIRECTIONAL_FLUID_PUMP_BE.get();
    }
    @Override public Direction.Axis getRotationAxis(BlockState state) {
        return shaftDirection(state).getAxis();
    }
    @Override public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction side) {
        return side.getAxis() == getRotationAxis(state);
    }
    @Override public boolean isSmallCog() { return false; }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
        net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.block();
    }
}
