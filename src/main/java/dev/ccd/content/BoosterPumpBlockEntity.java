package dev.ccd.content;

import java.util.List;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import dev.ccd.CCDConfig;
import dev.ccd.CopperDynamics;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        super.addToGoggleTooltip(tooltip, sneaking);
        tooltip.add(Component.translatable("ccd.booster_pump.range",
            CCDConfig.boosterRange(FluidPropagator.getPumpRange())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("ccd.booster_pump.pressure",
            CCDConfig.PUMP_PRESSURE_MULTIPLIER.get()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("ccd.booster_pump.ports").withStyle(ChatFormatting.DARK_GRAY));
        return true;
    }
}
