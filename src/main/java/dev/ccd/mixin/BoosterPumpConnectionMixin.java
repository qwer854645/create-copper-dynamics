package dev.ccd.mixin;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.ccd.CCDConfig;
import dev.ccd.content.BoosterPumpBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Pump port pressure must agree with pressure distributed to the surrounding pipes. */
@Mixin(targets = "com.simibubi.create.content.fluids.pump.PumpBlockEntity$PumpFluidTransferBehaviour")
abstract class BoosterPumpConnectionMixin extends FluidTransportBehaviour {
    protected BoosterPumpConnectionMixin(SmartBlockEntity be) { super(be); }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Ljava/lang/Math;abs(F)F"))
    private float ccd$boosterPortPressure(float rpm) {
        float original = Math.abs(rpm);
        if (blockEntity instanceof dev.ccd.content.ReciprocatingFluidPumpBlockEntity pump) return pump.flowPressure();
        return blockEntity instanceof BoosterPumpBlockEntity
            ? original * CCDConfig.PUMP_PRESSURE_MULTIPLIER.get().floatValue() : original;
    }
}
