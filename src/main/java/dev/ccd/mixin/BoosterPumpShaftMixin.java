package dev.ccd.mixin;

import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.ccd.content.BoosterPumpBlock;
import dev.ccd.content.BoosterPumpBlockEntity;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native gearbox conversion is restricted to its BE class. Adapt only CCD pump shaft ports. */
@Mixin(RotationPropagator.class)
abstract class BoosterPumpShaftMixin {
    @Inject(method = "getAxisModifier", at = @At("HEAD"), cancellable = true)
    private static void ccd$boosterShaftModifier(KineticBlockEntity be, Direction direction,
        CallbackInfoReturnable<Float> cir) {
        if (be instanceof BoosterPumpBlockEntity) {
            float factor = BoosterPumpBlock.shaftModifier(be.getBlockState(), direction);
            if (factor != 0) cir.setReturnValue(factor);
        }
    }
}
