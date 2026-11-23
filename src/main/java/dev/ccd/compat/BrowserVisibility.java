package dev.ccd.compat;

public final class BrowserVisibility {
    private BrowserVisibility() {}
    public static boolean hidden(Object ingredient) {
        if(ingredient instanceof net.minecraft.world.item.Item item && !dev.ccd.CCDFeatures.enabled(item))return true;
        if(ingredient instanceof net.minecraft.world.item.ItemStack item && !dev.ccd.CCDFeatures.enabled(item.getItem()))return true;
        if(ingredient instanceof net.minecraft.world.level.material.Fluid fluid && !dev.ccd.CCDFeatures.enabled(fluid))return true;
        if(ingredient instanceof net.neoforged.neoforge.fluids.FluidStack fluid && !dev.ccd.CCDFeatures.enabled(fluid.getFluid()))return true;
        for(var medium:ProcessingMedia.ALL) if(!medium.visible()) {
            if(ingredient==medium.bucket.get() || ingredient==medium.source.get() || ingredient==medium.flowing.get()) return true;
            if(ingredient instanceof net.minecraft.world.item.ItemStack stack && stack.is(medium.bucket.get())) return true;
            if(ingredient instanceof net.neoforged.neoforge.fluids.FluidStack fluid
                && fluid.getFluid().getFluidType()==medium.type.get()) return true;
        }
        return false;
    }
}
