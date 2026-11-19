package dev.ccd.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ccd.compat.CourierAmounts;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Adapt only CCD order quantities; never enter FluidLogistics-only class casts. */
@Pseudo
@Mixin(targets="de.theidler.create_mobile_packages.items.portable_stock_ticker.PortableStockTickerScreen",remap=false)
abstract class MobileTickerAmountMixin {
    @Shadow private java.util.List<java.util.List<Object>> displayedItems;
    @WrapOperation(method={"mouseClicked","mouseScrolled"},at=@At(value="INVOKE",
        target="Lru/zznty/create_factory_abstractions/generic/support/BigGenericStack;setAmount(I)V"))
    private void ccd$millibuckets(@Coerce Object stack,int amount,Operation<Void> original) {
        original.call(stack,CourierAmounts.portableChange(stack,amount,Screen.hasShiftDown(),Screen.hasControlDown(),displayedItems));
    }
}
