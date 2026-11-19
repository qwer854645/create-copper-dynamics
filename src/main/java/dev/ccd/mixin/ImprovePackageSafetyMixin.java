package dev.ccd.mixin;

import dev.ccd.compat.CourierPackages;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Extend Improve's existing safety decision, without replacing its inventory handling. */
@Pseudo
@Mixin(targets="com.molox.createimp.compat.fluidlogistics.FluidLogisticsCompat",remap=false)
abstract class ImprovePackageSafetyMixin {
    @Inject(method="blocksItemOnlyUnpack",at=@At("HEAD"),cancellable=true)
    private static void ccd$protectFluidCargo(ItemStack box,CallbackInfoReturnable<Boolean> cir) {
        if(CourierPackages.hasFluid(box)) cir.setReturnValue(true);
    }
}
