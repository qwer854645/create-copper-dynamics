package dev.ccd.mixin;

import java.util.List;
import com.simibubi.create.content.logistics.BigItemStack;
import dev.ccd.compat.CourierPackages;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.yision.phantom.compat.fluidlogistics.FluidLogisticsPackageCompat",remap=false)
abstract class PhantomPackagePreviewMixin {
    @Inject(method="getDisplayEntries",at=@At("HEAD"),cancellable=true)
    private static void ccd$fluidPreview(ItemStack box,CallbackInfoReturnable<List<BigItemStack>> cir) {
        if(CourierPackages.hasFluid(box)) cir.setReturnValue(CourierPackages.displayEntries(box));
    }
    @Inject(method="blocksManualOpen",at=@At("HEAD"),cancellable=true)
    private static void ccd$keepManualCargoSealed(ItemStack box,CallbackInfoReturnable<Boolean> cir) {
        if(CourierPackages.hasFluid(box)) cir.setReturnValue(true);
    }
}
