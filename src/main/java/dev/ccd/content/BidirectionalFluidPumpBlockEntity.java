package dev.ccd.content;

import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Native pressure magnitude and range; signed shaft speed selects the pull/push roles. */
public final class BidirectionalFluidPumpBlockEntity extends PumpBlockEntity {
    public BidirectionalFluidPumpBlockEntity(BlockPos pos, BlockState state) {
        super(CopperDynamics.BIDIRECTIONAL_FLUID_PUMP_BE.get(), pos, state);
    }
    @Override public boolean isPullingOnSide(boolean front) {
        return super.isPullingOnSide(front) ^ (getSpeed() < 0);
    }
    @Override public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        // Native pumps skip equal-magnitude reversals because their direction is state-driven.
        if (level != null && (!level.isClientSide() || isVirtual())
            && Math.abs(previousSpeed) == Math.abs(getSpeed()) && previousSpeed != getSpeed())
            updatePressureChange();
    }
    @Override public void initialize() {
        super.initialize();
        if (level != null && (!level.isClientSide() || isVirtual())) updatePressureChange();
    }
    @Override public void tick() {
        super.tick();
        if (level == null || level.isClientSide() || getSpeed() == 0 || isOverStressed()) return;
        boolean reversed = getSpeed() < 0;
        if (getBlockState().getValue(BidirectionalFluidPumpBlock.REVERSED) != reversed)
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(BidirectionalFluidPumpBlock.REVERSED, reversed));
    }
}
