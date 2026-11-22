package dev.ccd.mixin;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlazeBurnerBlockEntity.class)
abstract class TankBurnerAnimationMixin {
    @Inject(method="shouldTickAnimation",at=@At("HEAD"),cancellable=true)
    private void ccd$animateTankBurner(CallbackInfoReturnable<Boolean> cir){
        if((Object)this instanceof dev.ccd.content.TankBlazeBurnerBlockEntity)cir.setReturnValue(true);
    }
}
