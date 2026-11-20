package dev.ccd.content;

import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Keep native pressure, range and graph traversal; only reverse pull/push roles. */
public final class RedstoneFluidPumpBlockEntity extends PumpBlockEntity {
    public final net.createmod.catnip.animation.LerpedFloat pointer=net.createmod.catnip.animation.LerpedFloat.linear().startWithValue(0);
    public RedstoneFluidPumpBlockEntity(BlockPos pos,BlockState state) { super(CopperDynamics.REDSTONE_FLUID_PUMP_BE.get(),pos,state); }
    @Override public boolean isPullingOnSide(boolean front) { return super.isPullingOnSide(front)^getBlockState().getValue(RedstoneFluidPumpBlock.POWERED); }
    @Override public void initialize() { super.initialize();pointer.startWithValue(getBlockState().getValue(RedstoneFluidPumpBlock.POWERED)?1:0);if(level!=null && !level.isClientSide()) updatePressureChange(); }
    @Override public void tick() {
        super.tick();
        if(level!=null && (level.isClientSide() || isVirtual())) {
            // One half-turn in eight client ticks; renderer samples with partial ticks.
            // Chase from the current position so a second signal change reverses smoothly.
            pointer.chase(getBlockState().getValue(RedstoneFluidPumpBlock.POWERED)?1:0,.125,net.createmod.catnip.animation.LerpedFloat.Chaser.LINEAR);
            pointer.tickChaser();
        }
    }
}
