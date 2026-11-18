package dev.ccd.mixin;

import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import dev.ccd.atomizing.SandblastingType;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Redirect only CCD's quicksand medium to an installed sanding implementation. */
@Mixin(FanProcessingType.class)
interface QuicksandFanTypeMixin {
    @Inject(method="getAt",at=@At("RETURN"),cancellable=true)
    private static void ccd$quicksand(Level level,BlockPos pos,CallbackInfoReturnable<FanProcessingType> cir) {
        if(cir.getReturnValue()==CopperDynamics.SANDBLASTING.get()) cir.setReturnValue(SandblastingType.processingType());
    }
}
