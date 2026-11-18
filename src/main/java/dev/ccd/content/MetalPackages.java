package dev.ccd.content;

import dev.ccd.CopperDynamics;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Additional native sizes; the historical item IDs remain the 12x12 carriers. */
public final class MetalPackages {
    public record Variant(String id,String material,String size,DeferredHolder<Item,? extends Item> item) {}
    public static final List<Variant> VARIANTS=new ArrayList<>();
    static {
        for(String material:List.of("copper","brass")) for(String size:List.of("10x8","12x10","10x12")) {
            var parts=size.split("x"); int width=Integer.parseInt(parts[0]),height=Integer.parseInt(parts[1]);
            float hook=size.equals("10x8")?18F:size.equals("12x10")?21F:22F;
            String id=material+"_package_"+size;
            var item=CopperDynamics.ITEMS.register(id,()->material.equals("copper")
                ?new FluidPackageItem(new Item.Properties().stacksTo(1),width,height,hook)
                :new CompositePackageItem(new Item.Properties(),width,height,hook));
            VARIANTS.add(new Variant(id,material,size,item));
        }
    }
    public static void register() {}
}
