package dev.ccd.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

@Mixin(Entity.class)
abstract class SuperLavaEntityMixin {
    // NeoForge detects lava by FluidType identity, not the lava tag. Include our own type
    // in the vanilla height query so native movement, fire, fall and item rules apply.
    @Inject(method="getFluidTypeHeight",at=@At("RETURN"),cancellable=true)
    private void ccd$lavaHeight(net.neoforged.neoforge.fluids.FluidType type,CallbackInfoReturnable<Double> cir){
        if(type==net.neoforged.neoforge.common.NeoForgeMod.LAVA_TYPE.value()){
            var self=(Entity)(Object)this;
            cir.setReturnValue(Math.max(cir.getReturnValue(),self.getFluidTypeHeight(dev.ccd.compat.ProcessingMedia.ALL.getFirst().type.get())));
        }
    }
    @Inject(method="isInLava",at=@At("RETURN"),cancellable=true)
    private void ccd$inSuperLava(CallbackInfoReturnable<Boolean> cir){
        var self=(Entity)(Object)this;
        if(self.getFluidTypeHeight(dev.ccd.compat.ProcessingMedia.ALL.getFirst().type.get())>0)cir.setReturnValue(true);
    }
    @ModifyExpressionValue(method="lavaHurt",at=@At(value="CONSTANT",args="floatValue=4.0"))
    private float ccd$hotterDamage(float original){
        return ((Entity)(Object)this).getFluidTypeHeight(dev.ccd.compat.ProcessingMedia.ALL.getFirst().type.get())>0?20:original;
    }
}
