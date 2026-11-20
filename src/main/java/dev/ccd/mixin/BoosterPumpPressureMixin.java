package dev.ccd.mixin;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import dev.ccd.CCDConfig;
import dev.ccd.content.BoosterPumpBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keep the native graph walk and branch distribution; change only CCD's range and pressure inputs. */
@Mixin(PumpBlockEntity.class)
abstract class BoosterPumpPressureMixin {
    @Redirect(method = "distributePressureTo", at = @At(value = "INVOKE",
        target = "Lcom/simibubi/create/content/fluids/FluidPropagator;getPumpRange()I"))
    private int ccd$boosterRange() {
        int original = FluidPropagator.getPumpRange();
        if ((Object)this instanceof dev.ccd.content.ReciprocatingFluidPumpBlockEntity pump) return pump.targetRange();
        return (Object) this instanceof BoosterPumpBlockEntity ? CCDConfig.boosterRange(original) : original;
    }

    @Redirect(method = "distributePressureTo", at = @At(value = "INVOKE", target = "Ljava/lang/Math;abs(F)F"))
    private float ccd$boosterPressure(float rpm) {
        float original = Math.abs(rpm);
        if ((Object)this instanceof dev.ccd.content.ReciprocatingFluidPumpBlockEntity pump) return pump.flowPressure();
        return (Object) this instanceof BoosterPumpBlockEntity
            ? original * CCDConfig.PUMP_PRESSURE_MULTIPLIER.get().floatValue() : original;
    }
}
