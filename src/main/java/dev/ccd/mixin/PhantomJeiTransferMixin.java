package dev.ccd.mixin;

import dev.ccd.compat.jei.CourierFluidTransfer;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.simibubi.create.compat.jei.StockKeeperTransferHandler",remap=false)
abstract class PhantomJeiTransferMixin {
    @Inject(method="transferRecipeOnClient",at=@At("HEAD"),cancellable=true)
    private void ccd$phantomIngredients(StockKeeperRequestMenu menu,RecipeHolder<?> recipe,IRecipeSlotsView slots,
        Player player,boolean max,boolean execute,CallbackInfoReturnable<IRecipeTransferError> cir) {
        if(!menu.getClass().getName().startsWith("com.yision.phantom.")) return;
        var inputs=CourierFluidTransfer.ingredients(slots);
        if(inputs.isEmpty()) return;
        try {
            if(CourierFluidTransfer.stage(menu,inputs,execute)) cir.setReturnValue(null);
            else cir.setReturnValue(CourierFluidTransfer.error());
        } catch(ReflectiveOperationException | ArithmeticException error) { cir.setReturnValue(CourierFluidTransfer.error()); }
    }
}
