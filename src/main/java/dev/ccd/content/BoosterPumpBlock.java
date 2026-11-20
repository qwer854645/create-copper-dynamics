package dev.ccd.content;

import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The facing axis remains a native pump's fluid axis; the other four faces carry shafts. */
public final class BoosterPumpBlock extends PumpBlock {
    public BoosterPumpBlock(Properties properties) { super(properties); }

    @Override public BlockEntityType<? extends PumpBlockEntity> getBlockEntityType() {
        return CopperDynamics.BOOSTER_PUMP_BE.get();
    }

    @Override public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() != state.getValue(FACING).getAxis();
    }

    @Override public boolean isSmallCog() { return false; }

    // PumpBlock supplies placement, waterlogging and wrench reversal without CCD overrides.

    @Override public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    /** Fixed internal shaft convention. Opposite ports rotate in opposite local directions;
     * the two perpendicular shaft axes mesh at 1:1. Network propagation and rendering share it. */
    public static float shaftModifier(BlockState state, Direction face) {
        if (state.getBlock() instanceof TurningBoosterPumpBlock) return TurningBoosterPumpBlock.shaftModifier(state, face);
        Direction.Axis pipeAxis = state.getValue(FACING).getAxis();
        if (face.getAxis() == pipeAxis) return 0;
        Direction.Axis firstShaftAxis = pipeAxis == Direction.Axis.X ? Direction.Axis.Y : Direction.Axis.X;
        int side = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1;
        return face.getAxis() == firstShaftAxis ? side : -side;
    }
}
