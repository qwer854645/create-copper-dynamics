package dev.ccd.mixin.pnp;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A storage mouth touching only a terminal is not a PnP pipe network. */
@Pseudo @Mixin(targets="de.devin.pipesnphysics.engine.EngineTickHandler",remap=false)
abstract class DirectSourceTransportMixin {
    @Inject(method="suppressesCreateTransport",at=@At("HEAD"),cancellable=true)
    private static void ccd$directSource(SmartBlockEntity be,CallbackInfoReturnable<Boolean> cir){
        var transport=be.getBehaviour(FluidTransportBehaviour.TYPE);
        if(transport instanceof dev.ccd.logistics.StoredSourceTransport source&&source.directNative())cir.setReturnValue(false);
    }
}
