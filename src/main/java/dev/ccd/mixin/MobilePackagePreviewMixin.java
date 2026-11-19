package dev.ccd.mixin;

import dev.ccd.compat.CourierPackages;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets="de.theidler.create_mobile_packages.robo.PlayerTarget",remap=false)
abstract class MobilePackagePreviewMixin {
    @Redirect(method="updateEtaToast",at=@At(value="INVOKE",target="Lcom/simibubi/create/content/logistics/box/PackageItem;getContents(Lnet/minecraft/world/item/ItemStack;)Lnet/neoforged/neoforge/items/ItemStackHandler;"))
    private ItemStackHandler ccd$previewFluidContents(ItemStack box) {
        return CourierPackages.toastContents(box);
    }
}
