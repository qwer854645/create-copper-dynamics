package dev.ccd.mixin;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import dev.ccd.content.FluidPackageItem;
import dev.ccd.content.CompositePackageItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native item unpacking treats zero item contents as an empty, consumable box. */
@Mixin(PackagerBlockEntity.class)
abstract class PackagerGuardMixin {
    @Inject(method = "unwrapBox", at = @At("HEAD"), cancellable = true)
    private void ccd$rejectFluidInItemUnpacker(ItemStack box, boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        if (CompositePackageItem.isSealed(box)) cir.setReturnValue(false);
    }
}
