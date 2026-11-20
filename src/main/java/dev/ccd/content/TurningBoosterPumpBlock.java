package dev.ccd.content;

import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import com.simibubi.create.content.fluids.pump.PumpBlock;

/** FACING and branch are fixed physical ports; reversal never moves either port. */
public final class TurningBoosterPumpBlock extends PumpBlock implements com.simibubi.create.api.contraption.transformable.TransformableBlock {
    public static final IntegerProperty ROLL = IntegerProperty.create("roll", 0, 3);
    public static final BooleanProperty REVERSED = BooleanProperty.create("reversed");
    public TurningBoosterPumpBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(ROLL, 0).setValue(REVERSED, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ROLL, REVERSED);
    }
    public static Direction branch(BlockState state) {
        Direction front = state.getValue(FACING);
        Direction side = front.getAxis().isVertical() ? Direction.NORTH : Direction.UP;
        for (int i = 0; i < state.getValue(ROLL); i++) side = side.getClockWise(front.getAxis());
        return side;
    }
    public static boolean open(BlockState state, Direction side) {
        return side == state.getValue(FACING) || side == branch(state);
    }
    public static float shaftModifier(BlockState state, Direction side) {
        if (side == state.getValue(FACING).getOpposite()) return 1;
        if (side == branch(state).getOpposite()) return -1;
        return 0;
    }
    private static BlockState oriented(BlockState state, Direction front, Direction branch) {
        state = state.setValue(FACING, front);
        for (int roll = 0; roll < 4; roll++) {
            var candidate = state.setValue(ROLL, roll);
            if (branch(candidate) == branch) return candidate;
        }
        throw new IllegalArgumentException("Turning pump ports must be perpendicular");
    }
    @Override public BlockState getRotatedBlockState(BlockState state, Direction clickedFace) {
        return oriented(state, state.getValue(FACING).getClockWise(clickedFace.getAxis()),
            branch(state).getClockWise(clickedFace.getAxis()));
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return oriented(state, rotation.rotate(state.getValue(FACING)), rotation.rotate(branch(state)));
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        return oriented(state, mirror.mirror(state.getValue(FACING)), mirror.mirror(branch(state)));
    }
    @Override public BlockState transform(BlockState state, com.simibubi.create.content.contraptions.StructureTransform transform) {
        if (transform.mirror != null) state = mirror(state, transform.mirror);
        return oriented(state, transform.rotateFacing(state.getValue(FACING)), transform.rotateFacing(branch(state)));
    }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide() && state != old && level.getBlockEntity(pos) instanceof TurningBoosterPumpBlockEntity pump) {
            if (old.getBlock() instanceof TurningBoosterPumpBlock) {
                for (Direction side : new Direction[]{old.getValue(FACING), branch(old)}) {
                    var neighbour = pos.relative(side);
                    com.simibubi.create.content.fluids.FluidPropagator.propagateChangedPipe(level, neighbour, level.getBlockState(neighbour));
                }
            }
            pump.updatePressureChange();
        }
    }
    @Override public BlockEntityType<? extends PumpBlockEntity> getBlockEntityType() { return CopperDynamics.TURNING_BOOSTER_PUMP_BE.get(); }
    @Override public Direction.Axis getRotationAxis(BlockState state) { return state.getValue(FACING).getAxis(); }
    @Override public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction side) {
        return shaftModifier(state, side) != 0;
    }
    @Override public boolean isSmallCog() { return false; }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
        BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) { return net.minecraft.world.phys.shapes.Shapes.block(); }
}
