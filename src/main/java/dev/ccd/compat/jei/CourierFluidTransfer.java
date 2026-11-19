package dev.ccd.compat.jei;

import java.util.ArrayList;
import java.util.List;
import com.simibubi.create.content.logistics.BigItemStack;
import dev.ccd.content.FluidStockItem;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.neoforge.NeoForgeTypes;
import net.minecraft.world.item.ItemStack;

/** Stage one recipe's ingredients as a request, never claim automated crafting. */
public final class CourierFluidTransfer {
    private CourierFluidTransfer() {}
    public static mezz.jei.api.recipe.transfer.IRecipeTransferError error() {
        return new mezz.jei.api.recipe.transfer.IRecipeTransferError() {
            @Override public Type getType() { return Type.USER_FACING; }
            @Override public void getTooltip(mezz.jei.api.gui.builder.ITooltipBuilder tooltip) {
                tooltip.add(net.minecraft.network.chat.Component.translatable("ccd.courier.transfer_unavailable"));
            }
        };
    }
    public static List<BigItemStack> ingredients(IRecipeSlotsView slots) {
        var inputs=slots.getSlotViews(RecipeIngredientRole.INPUT);
        if(inputs.stream().noneMatch(s->s.getDisplayedIngredient(NeoForgeTypes.FLUID_STACK).isPresent())) return List.of();
        var result=new ArrayList<BigItemStack>();
        for(var slot:inputs) {
            var fluid=slot.getDisplayedIngredient(NeoForgeTypes.FLUID_STACK);
            ItemStack key; int count;
            if(fluid.isPresent()) { key=FluidStockItem.of(fluid.get()); count=fluid.get().getAmount(); }
            else {
                var item=slot.getDisplayedIngredient(VanillaTypes.ITEM_STACK);
                if(item.isEmpty()) continue;
                key=item.get().copyWithCount(1); count=item.get().getCount();
            }
            if(key.isEmpty() || count<=0) continue;
            var existing=result.stream().filter(e->ItemStack.isSameItemSameComponents(e.stack,key)).findFirst();
            if(existing.isPresent()) existing.get().count=Math.addExact(existing.get().count,count);
            else result.add(new BigItemStack(key,count));
        }
        return result;
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    public static boolean stage(Object menu,List<BigItemStack> inputs,boolean execute) throws ReflectiveOperationException {
        Object screen=menu.getClass().getField("screenReference").get(menu);
        if(screen==null) return false;
        List orders=(List)screen.getClass().getField("itemsToOrder").get(screen);
        boolean generic=!screen.getClass().getName().startsWith("com.yision.phantom.");
        Class<?> api=generic?Class.forName("ru.zznty.create_factory_abstractions.generic.support.BigGenericStack"):null;
        List<BigItemStack> merged=new ArrayList<>();
        for(Object entry:orders) {
            var item=(BigItemStack)(generic?api.getMethod("asStack").invoke(entry):entry);
            merged.add(new BigItemStack(item.stack.copy(),item.count));
        }
        for(var input:inputs) {
            var existing=merged.stream().filter(e->ItemStack.isSameItemSameComponents(e.stack,input.stack)).findFirst();
            if(existing.isPresent()) existing.get().count=Math.addExact(existing.get().count,input.count);
            else merged.add(new BigItemStack(input.stack.copy(),input.count));
        }
        if(merged.size()>9) return false;
        if(execute) {
            List replacement=new ArrayList();
            for(var item:merged) {
                if(!generic) replacement.add(item);
                else {
                    Class<?> gs=Class.forName("ru.zznty.create_factory_abstractions.api.generic.stack.GenericStack");
                    Object wrapped=gs.getMethod("wrap",ItemStack.class).invoke(null,item.stack);
                    wrapped=gs.getMethod("withAmount",int.class).invoke(wrapped,item.count);
                    replacement.add(api.getMethod("of",gs).invoke(null,wrapped));
                }
            }
            orders.clear(); orders.addAll(replacement);
        }
        return true;
    }
}
