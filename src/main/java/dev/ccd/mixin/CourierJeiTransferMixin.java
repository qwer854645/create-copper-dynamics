package dev.ccd.mixin;

import dev.ccd.compat.jei.CourierFluidTransfer;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="de.theidler.create_mobile_packages.compat.jei.DroneControllerTransferHandler",remap=false)
abstract class CourierJeiTransferMixin {
    @Inject(method="transferRecipeOnClient",at=@At("HEAD"),cancellable=true)
    private void ccd$requestFluidIngredients(@Coerce Object menu,RecipeHolder<?> recipe,IRecipeSlotsView slots,
        Player player,boolean maxTransfer,boolean execute,CallbackInfoReturnable<IRecipeTransferError> cir) {
        var ingredients=CourierFluidTransfer.ingredients(slots);
        if(ingredients.isEmpty()) return;
        try {
            if(CourierFluidTransfer.stage(menu,ingredients,execute)) { cir.setReturnValue(null); return; }
        } catch(ReflectiveOperationException | ArithmeticException ignored) { }
        cir.setReturnValue(CourierFluidTransfer.error());
    }
}
