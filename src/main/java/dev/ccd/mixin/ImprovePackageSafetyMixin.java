package dev.ccd.mixin;

import dev.ccd.compat.CourierPackages;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Extend Improve 1.4.5's safety decision when present; older releases lack this hook. */
@Pseudo
@Mixin(targets="com.molox.createimp.compat.fluidlogistics.FluidLogisticsCompat",remap=false)
abstract class ImprovePackageSafetyMixin {
    @Inject(method="blocksItemOnlyUnpack",at=@At("HEAD"),cancellable=true,require=0)
    private static void ccd$protectFluidCargo(ItemStack box,CallbackInfoReturnable<Boolean> cir) {
        if(CourierPackages.hasFluid(box)) cir.setReturnValue(true);
    }
}
