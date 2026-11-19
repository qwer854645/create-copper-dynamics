package dev.ccd.content;

import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class BoosterPumpBlockEntity extends PumpBlockEntity {
    public BoosterPumpBlockEntity(BlockPos pos, BlockState state) {
        super(CopperDynamics.BOOSTER_PUMP_BE.get(), pos, state);
    }

    @Override public void initialize() {
        super.initialize();
        // Native speed-change detection can skip unchanged saved RPM; rebuild extended pressure after loading.
        if (level != null && (!level.isClientSide() || isVirtual())) updatePressureChange();
    }

}
