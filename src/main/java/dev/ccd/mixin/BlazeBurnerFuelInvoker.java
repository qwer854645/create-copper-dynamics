package dev.ccd.mixin;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlazeBurnerBlockEntity.class)
public interface BlazeBurnerFuelInvoker {
    @Invoker("tryUpdateFuel") boolean ccd$tryFuel(ItemStack stack,boolean overflow,boolean simulate);
}
