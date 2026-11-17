package dev.ccd.mixin;

import dev.ccd.client.FluidCanisterModel;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Change only the native model lookup; native animation, lighting and rigging remain intact. */
@Mixin(com.simibubi.create.content.logistics.box.PackageVisual.class)
abstract class CanisterEntityVisualMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/core/DefaultedRegistry;getKey(Ljava/lang/Object;)Lnet/minecraft/resources/ResourceLocation;"))
    private ResourceLocation ccd$fluidAppearance(DefaultedRegistry<Item> registry, Object item,
        dev.engine_room.flywheel.api.visualization.VisualizationContext context, com.simibubi.create.content.logistics.box.PackageEntity entity, float pt) {
        return FluidCanisterModel.transportKey(entity.box, (Item) item);
    }
}
