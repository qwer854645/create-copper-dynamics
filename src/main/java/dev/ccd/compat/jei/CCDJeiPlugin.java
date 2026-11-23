package dev.ccd.compat.jei;

import dev.ccd.compat.ProcessingMedia;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.neoforge.NeoForgeTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.ArrayList;

@JeiPlugin
public final class CCDJeiPlugin implements IModPlugin {
    @Override public void registerGuiHandlers(mezz.jei.api.registration.IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(dev.ccd.client.FluidFilterScreen.class, new mezz.jei.api.gui.handlers.IGhostIngredientHandler<dev.ccd.client.FluidFilterScreen>() {
            @Override public <I> java.util.List<Target<I>> getTargetsTyped(dev.ccd.client.FluidFilterScreen screen,
                mezz.jei.api.ingredients.ITypedIngredient<I> ingredient, boolean start) {
                FluidStack fluid = ingredient.getIngredient(NeoForgeTypes.FLUID_STACK).map(FluidStack::copy)
                    .orElseGet(() -> ingredient.getIngredient(VanillaTypes.ITEM_STACK).map(dev.ccd.content.FluidStockItem::sample).orElse(FluidStack.EMPTY));
                if (fluid.isEmpty()) return java.util.List.of();
                var targets = new java.util.ArrayList<Target<I>>();
                for (int i=0;i<screen.targetCount();i++) {
                    final int slot=i;
                    targets.add(new Target<I>() {
                        @Override public net.minecraft.client.renderer.Rect2i getArea() { return screen.target(slot); }
                        @Override public void accept(I ignored) { screen.acceptFluid(slot, fluid); }
                    });
                }
                return targets;
            }
            @Override public void onComplete() {}
        });
    }
    @Override public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath("ccd","visibility"); }
    @Override public void onRuntimeAvailable(IJeiRuntime runtime) {
        var items=new ArrayList<ItemStack>(); var fluids=new ArrayList<FluidStack>();
        for(var item:net.minecraft.core.registries.BuiltInRegistries.ITEM)
            if(!dev.ccd.CCDFeatures.enabled(item))items.add(new ItemStack(item));
        for(var fluid:net.minecraft.core.registries.BuiltInRegistries.FLUID)
            if(!dev.ccd.CCDFeatures.enabled(fluid))fluids.add(new FluidStack(fluid,1000));
        for(var medium:ProcessingMedia.ALL) if(!medium.visible()) {
            items.add(new ItemStack(medium.bucket.get()));
            fluids.add(new FluidStack(medium.source.get(),1000));
            fluids.add(new FluidStack(medium.flowing.get(),1000));
        }
        runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK,items);
        runtime.getIngredientManager().removeIngredientsAtRuntime(NeoForgeTypes.FLUID_STACK,fluids);
    }
}
