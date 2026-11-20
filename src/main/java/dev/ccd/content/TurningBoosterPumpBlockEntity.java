package dev.ccd.content;

import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public final class TurningBoosterPumpBlockEntity extends BoosterPumpBlockEntity {
    public TurningBoosterPumpBlockEntity(BlockPos pos, BlockState state) {
        super(CopperDynamics.TURNING_BOOSTER_PUMP_BE.get(), pos, state);
    }
    @Override public boolean isSideAccessible(Direction side) { return TurningBoosterPumpBlock.open(getBlockState(), side); }
    @Override public boolean isPullingOnSide(boolean front) {
        return super.isPullingOnSide(front) ^ getBlockState().getValue(TurningBoosterPumpBlock.REVERSED);
    }
}
