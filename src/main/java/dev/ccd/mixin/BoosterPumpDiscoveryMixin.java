package dev.ccd.mixin;

import com.simibubi.create.content.fluids.FluidPropagator;
import dev.ccd.CCDConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** A far pipe edit must still find a booster before its previously unpressurized section is connected. */
@Mixin(FluidPropagator.class)
abstract class BoosterPumpDiscoveryMixin {
    @Redirect(method = "propagateChangedPipe", at = @At(value = "INVOKE",
        target = "Lcom/simibubi/create/content/fluids/FluidPropagator;getPumpRange()I"))
    private static int ccd$extendedPumpDiscovery() {
        return Math.max(CCDConfig.boosterRange(FluidPropagator.getPumpRange()),
            Math.max(CCDConfig.RECIPROCATING_MIN_RANGE.get(), CCDConfig.RECIPROCATING_MAX_RANGE.get()));
    }
}
