package dev.ccd.mixin.pnp;

import dev.ccd.compat.pnp.PnPCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** CCD pumps and storage outlets have no turbine mode, including internal PnP callbacks. */
@Pseudo
@Mixin(targets="com.simibubi.create.content.fluids.pump.PumpBlockEntity")
abstract class ComponentPumpModeMixin {
    @Inject(method="pipesnphysics$isTurbine",at=@At("HEAD"),cancellable=true,require=0,remap=false)
    private void ccd$pumpOnly(CallbackInfoReturnable<Boolean> cir) {
        var be=(net.minecraft.world.level.block.entity.BlockEntity)(Object)this;
        if(PnPCompat.pump(be)||PnPCompat.endpoint(be))cir.setReturnValue(false);
    }
}
