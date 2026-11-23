package dev.ccd.content;

import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BoosterPumpBlockEntity extends PumpBlockEntity {
    @Override protected void distributePressureTo(net.minecraft.core.Direction side) {
        if(dev.ccd.CCDFeatures.enabled(getBlockState()))super.distributePressureTo(side);
    }
    public BoosterPumpBlockEntity(BlockPos pos, BlockState state) {
        this(CopperDynamics.BOOSTER_PUMP_BE.get(), pos, state);
    }
    protected BoosterPumpBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override public void initialize() {
        super.initialize();
        // Native speed-change detection can skip unchanged saved RPM; rebuild extended pressure after loading.
        if (level != null && (!level.isClientSide() || isVirtual())) updatePressureChange();
    }

    @Override public void tick() {
        if (!dev.ccd.CCDFeatures.enabled(getBlockState())) return;
        super.tick();
    }
}
