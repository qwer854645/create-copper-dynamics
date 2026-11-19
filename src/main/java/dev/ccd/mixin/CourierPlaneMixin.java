package dev.ccd.mixin;

import dev.ccd.compat.CourierPackages;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Deliver sealed CCD cargo; native automatic item unpacking cannot read fluid components. */
@Pseudo
@Mixin(targets="com.kreidev.cmpackagecouriers.plane.CardboardPlane",remap=false)
abstract class CourierPlaneMixin {
    @Shadow public ItemStack box;
    @Shadow public boolean unpack;
    @Inject(method="onReachedTarget",at=@At("HEAD"))
    private void ccd$keepFluidSealed(MinecraftServer server,CallbackInfo ci) {
        if (CourierPackages.hasFluid(box)) unpack=false;
    }
}
