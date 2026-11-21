package dev.ccd.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.ccd.compat.BrowserVisibility;

@EmiEntrypoint
public final class CCDEmiPlugin implements EmiPlugin {
    @Override public void register(EmiRegistry registry) {
        registry.addDragDropHandler(dev.ccd.client.FluidFilterScreen.class, (screen, ingredient, x, y) -> {
            for (var stack : ingredient.getEmiStacks()) {
                net.neoforged.neoforge.fluids.FluidStack fluid;
                if (stack.getKey() instanceof net.minecraft.world.level.material.Fluid source)
                    fluid = new net.neoforged.neoforge.fluids.FluidStack(source, 1);
                else fluid = dev.ccd.content.FluidStockItem.sample(stack.getItemStack());
                if (fluid.isEmpty()) continue;
                for (int i=0;i<screen.targetCount();i++) if(screen.target(i).contains(x,y)) {
                    screen.acceptFluid(i,fluid); return true;
                }
            }
            return false;
        });
        registry.removeEmiStacks(stack->BrowserVisibility.hidden(stack.getKey()));
    }
}
