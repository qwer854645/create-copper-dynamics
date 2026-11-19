package dev.ccd.mixin;

import dev.ccd.content.FluidStockItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.kreidev.cmpackagecouriers.compat.fluidlogistics.CFLClientBridge",remap=false)
abstract class CourierAmountDisplayMixin {
    @Inject(method="tryRenderStockKeeperAmount",at=@At("HEAD"),cancellable=true)
    private static void ccd$mb(GuiGraphics graphics,ItemStack stack,int amount,CallbackInfoReturnable<Boolean> cir) {
        if(!(stack.getItem() instanceof FluidStockItem)) return;
        String text=amount+" mB";
        var font=Minecraft.getInstance().font;
        graphics.pose().pushPose();
        graphics.pose().scale(.5f,.5f,.5f);
        graphics.drawString(font,text,32-font.width(text),24,0xffffff,true);
        graphics.pose().popPose();
        cir.setReturnValue(true);
    }
}
