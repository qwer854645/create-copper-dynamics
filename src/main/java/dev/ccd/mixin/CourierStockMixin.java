package dev.ccd.mixin;

import dev.ccd.compat.CourierAmounts;
import dev.ccd.content.FluidStockItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.kreidev.cmpackagecouriers.compat.fluidlogistics.CFLBridge",remap=false)
abstract class CourierStockMixin {
    @Inject(method="isPackageResource(Lnet/minecraft/world/item/ItemStack;)Z",at=@At("HEAD"),cancellable=true)
    private static void ccd$token(ItemStack stack,CallbackInfoReturnable<Boolean> cir) {
        if(stack.getItem() instanceof FluidStockItem) cir.setReturnValue(true);
    }
    @Inject(method="adjustAmount",at=@At("HEAD"),cancellable=true)
    private static void ccd$amount(ItemStack stack,int current,boolean add,boolean shift,boolean control,
        int minimum,int available,int steps,@Coerce Object interaction,CallbackInfoReturnable<Integer> cir) {
        if(stack.getItem() instanceof FluidStockItem)
            cir.setReturnValue(CourierAmounts.adjust(current,add,shift,control,minimum,available,steps));
    }
}
