package dev.ccd.logistics;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.function.Predicate;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.AllRecipeTypes;

/** Preview native recipe selection on copies, without rolling recipe outputs. */
public final class FillingOutputFilter {
    private FillingOutputFilter() {}
    public static boolean accepts(Level level,ItemStack item,FluidStack fluid,int required,Predicate<ItemStack> filter){
        var input=new net.minecraft.world.item.crafting.SingleRecipeInput(item.copy());
        var assembly=com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe.getRecipe(level,input,
            AllRecipeTypes.FILLING.getType(),FillingRecipe.class,
            holder->holder.value().matches(input,level)&&holder.value().getRequiredFluid().test(fluid));
        if(assembly.isPresent())return assembly.get().value().getRollableResults().stream().anyMatch(o->o.getChance()>0&&filter.test(o.getStack()));
        net.minecraft.world.item.crafting.RecipeType<FillingRecipe> type=AllRecipeTypes.FILLING.getType();
        var recipe=level.getRecipeManager().getAllRecipesFor(type).stream().map(net.minecraft.world.item.crafting.RecipeHolder::value)
            .filter(r->r.matches(input,level)&&r.getRequiredFluid().test(fluid)).findFirst();
        if(recipe.isPresent())return recipe.get().getRollableResults().stream().anyMatch(o->o.getChance()>0&&filter.test(o.getStack()));
        var result=com.simibubi.create.content.fluids.transfer.GenericItemFilling.fillItem(level,required,item.copy(),fluid.copy());
        return !result.isEmpty()&&filter.test(result);
    }
}
