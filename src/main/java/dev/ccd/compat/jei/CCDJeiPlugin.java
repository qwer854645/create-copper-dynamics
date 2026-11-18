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
    @Override public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath("ccd","visibility"); }
    @Override public void onRuntimeAvailable(IJeiRuntime runtime) {
        var items=new ArrayList<ItemStack>(); var fluids=new ArrayList<FluidStack>();
        for(var medium:ProcessingMedia.ALL) if(!medium.available()) {
            items.add(new ItemStack(medium.bucket.get()));
            fluids.add(new FluidStack(medium.source.get(),1000));
            fluids.add(new FluidStack(medium.flowing.get(),1000));
        }
        runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK,items);
        runtime.getIngredientManager().removeIngredientsAtRuntime(NeoForgeTypes.FLUID_STACK,fluids);
    }
}
