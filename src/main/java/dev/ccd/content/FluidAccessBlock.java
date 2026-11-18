package dev.ccd.content;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;

/** FACING points away from the attached container; bridges use both axial faces. */
public final class FluidAccessBlock extends DirectionalBlock implements IBE<FluidAccessBlockEntity>, IWrenchable,
    com.simibubi.create.api.contraption.transformable.TransformableBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty ATTACHED_NEGATIVE = BooleanProperty.create("attached_negative");
    public static final BooleanProperty ATTACHED_POSITIVE = BooleanProperty.create("attached_positive");
    private final boolean bridge;
    private final MapCodec<FluidAccessBlock> codec;
    public FluidAccessBlock(Properties properties, boolean bridge) {
        super(properties);
        this.bridge = bridge;
        codec = simpleCodec(p -> new FluidAccessBlock(p, bridge));
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWERED, false)
            .setValue(ATTACHED_NEGATIVE, false).setValue(ATTACHED_POSITIVE, false));
    }
    public boolean isBridge() { return bridge; }
    @Override protected MapCodec<? extends DirectionalBlock> codec() { return codec; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, ATTACHED_NEGATIVE, ATTACHED_POSITIVE);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction target = null;
        for (Direction candidate : context.getNearestLookingDirections()) {
            BlockPos neighbour = context.getClickedPos().relative(candidate);
            if (context.getLevel().hasChunkAt(neighbour) && context.getLevel().getBlockEntity(neighbour) != null
                && context.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, neighbour, null) != null) {
                target = candidate;
                break;
            }
        }
        if (target == null) {
            target = context.getNearestLookingDirection();
            if (!bridge && (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown()))
                target = target.getOpposite();
        }
        return defaultBlockState().setValue(FACING, bridge ? axialFacing(target) : target.getOpposite());
    }
    private static Direction axialFacing(Direction direction) {
        return Direction.fromAxisAndDirection(direction.getAxis(), Direction.AxisDirection.NEGATIVE);
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    @Override public BlockState transform(BlockState state, com.simibubi.create.content.contraptions.StructureTransform transform) {
        if (transform.mirror != null) state = mirror(state, transform.mirror);
        Direction facing = transform.rotateFacing(state.getValue(FACING));
        return state.setValue(FACING, bridge ? axialFacing(facing) : facing);
    }
    @Override public BlockState getRotatedBlockState(BlockState state, Direction face) {
        Direction facing = state.getValue(FACING);
        if (facing.getAxis() == face.getAxis()) return state;
        Direction turned = facing.getClockWise(face.getAxis());
        if (!bridge && face.getAxis() != Direction.Axis.Y && face.getAxisDirection() == Direction.AxisDirection.NEGATIVE)
            turned = facing.getCounterClockWise(face.getAxis());
        return state.setValue(FACING, bridge ? axialFacing(turned) : turned);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        IBE.onRemove(state, level, pos, next);
        super.onRemove(state, level, pos, next, moving);
    }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide()) refreshConnections(level, pos);
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (!level.isClientSide() && !level.getBlockTicks().hasScheduledTick(pos, this)) level.scheduleTick(pos, this, 1);
    }
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        refreshConnections(level, pos);
    }
    public void refreshConnections(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(this)) return;
        Direction facing = state.getValue(FACING);
        boolean powered = level.hasNeighborSignal(pos);
        Direction negative = bridge ? Direction.fromAxisAndDirection(facing.getAxis(), Direction.AxisDirection.NEGATIVE) : facing.getOpposite();
        boolean first = !powered && dev.ccd.logistics.FluidAccess.hasDirectContainer(level, pos, negative);
        boolean second = bridge && !powered && dev.ccd.logistics.FluidAccess.hasDirectContainer(level, pos, negative.getOpposite());
        BlockState next = state.setValue(POWERED, powered).setValue(ATTACHED_NEGATIVE, first).setValue(ATTACHED_POSITIVE, second);
        if (next != state) level.setBlock(pos, next, Block.UPDATE_ALL);
        level.invalidateCapabilities(pos);
        level.updateNeighbourForOutputSignal(pos, this);
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.hasNeighborSignal(pos)) return 0;
        Direction facing = state.getValue(FACING);
        Direction first = bridge ? Direction.fromAxisAndDirection(facing.getAxis(), Direction.AxisDirection.NEGATIVE) : facing.getOpposite();
        int signal = containerSignal(level, pos, first);
        return bridge ? (signal + containerSignal(level, pos, first.getOpposite())) / 2 : signal;
    }
    private int containerSignal(Level level, BlockPos pos, Direction end) {
        if (!dev.ccd.logistics.FluidAccess.hasDirectContainer(level, pos, end)) return 0;
        BlockPos target = pos.relative(end);
        BlockState state = level.getBlockState(target);
        return state.hasAnalogOutputSignal() ? state.getAnalogOutputSignal(level, target) : 0;
    }
    @Override public Class<FluidAccessBlockEntity> getBlockEntityClass() { return FluidAccessBlockEntity.class; }
    @Override public BlockEntityType<? extends FluidAccessBlockEntity> getBlockEntityType() {
        return bridge ? CopperDynamics.FLUID_BRIDGE_BE.get() : CopperDynamics.FLUID_ACCESS_PORT_BE.get();
    }
}
