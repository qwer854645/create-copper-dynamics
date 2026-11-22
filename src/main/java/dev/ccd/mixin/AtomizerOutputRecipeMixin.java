package dev.ccd.mixin;

import com.simibubi.create.foundation.recipe.RecipeApplier;
import dev.ccd.atomizing.AtomizerProcessing;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Check declared recipe outputs before rolling chances or consuming the input. */
@Mixin(RecipeApplier.class)
abstract class AtomizerOutputRecipeMixin {
    @Inject(method="applyRecipeOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/crafting/Recipe;Z)Ljava/util/List;",
        at=@At("HEAD"),cancellable=true)
    private static void ccd$target(Level level,ItemStack input,Recipe<?> recipe,boolean simulate,
        CallbackInfoReturnable<java.util.List<ItemStack>> cir) {
        var owner=AtomizerProcessing.source();
        if(owner!=null&&!owner.acceptsRecipe(recipe))cir.setReturnValue(null);
    }
}
