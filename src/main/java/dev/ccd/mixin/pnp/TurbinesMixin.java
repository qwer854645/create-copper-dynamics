package dev.ccd.mixin.pnp;

import dev.ccd.compat.pnp.PnPCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo @Mixin(targets="de.devin.pipesnphysics.engine.turbine.Turbines",remap=false)
abstract class TurbinesMixin {
    @Inject(method="isTurbine",at=@At("HEAD"),cancellable=true)
    private static void ccd$noAutoTurbine(Level level,BlockPos pos,CallbackInfoReturnable<Boolean> cir) {
        if(PnPCompat.pump(level.getBlockEntity(pos)))cir.setReturnValue(false);
    }
}
