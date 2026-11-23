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
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="canInteract",at=@At(value="INVOKE",target="Lnet/neoforged/neoforge/fluids/FluidInteractionRegistry$HasFluidInteraction;test(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/material/FluidState;)Z"))
    private static boolean ccd$cancelOriginal(net.neoforged.neoforge.fluids.FluidInteractionRegistry.HasFluidInteraction predicate,
        Level level,BlockPos pos,BlockPos neighbor,net.minecraft.world.level.material.FluidState state,
        com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original){
        return !FluidWorldScripts.cancels(level,pos,neighbor,state)&&original.call(predicate,level,pos,neighbor,state);
    }
    @Inject(method="canInteract",at=@At("HEAD"),cancellable=true)
    private static void ccd$scriptPair(Level level,BlockPos pos,CallbackInfoReturnable<Boolean> cir){
        if(FluidWorldScripts.blockContact(level,pos)||FluidWorldScripts.adjacent(level,pos))cir.setReturnValue(true);
    }
}
