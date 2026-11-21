package dev.ccd.mixin;

import dev.ccd.api.FluidWorldScripts;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.neoforged.neoforge.fluids.FluidInteractionRegistry.class)
abstract class ScriptFluidInteractionMixin {
    @Inject(method="canInteract",at=@At("HEAD"),cancellable=true)
    private static void ccd$scriptPair(Level level,BlockPos pos,CallbackInfoReturnable<Boolean> cir){
        if(FluidWorldScripts.adjacent(level,pos))cir.setReturnValue(true);
    }
}
