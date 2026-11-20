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
public final class BidirectionalFluidPumpBlock extends PumpBlock {
    public BidirectionalFluidPumpBlock(Properties properties) { super(properties); }
    @Override public BlockEntityType<? extends PumpBlockEntity> getBlockEntityType() {
        return CopperDynamics.BIDIRECTIONAL_FLUID_PUMP_BE.get();
    }
    @Override public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis().isVertical() ? Direction.Axis.Z : Direction.Axis.Y;
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
