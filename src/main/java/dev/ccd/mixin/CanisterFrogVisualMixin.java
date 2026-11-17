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
@Mixin(com.simibubi.create.content.logistics.packagePort.frogport.FrogportVisual.class)
abstract class CanisterFrogVisualMixin extends dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual<com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity> {
    protected CanisterFrogVisualMixin(dev.engine_room.flywheel.api.visualization.VisualizationContext context,
        com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity entity, float pt) {
        super(context, entity, pt);
    }
    @Redirect(method = "renderPackage", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/core/DefaultedRegistry;getKey(Ljava/lang/Object;)Lnet/minecraft/resources/ResourceLocation;"))
    private ResourceLocation ccd$fluidAppearance(DefaultedRegistry<Item> registry, Object item,
        net.minecraft.world.phys.Vec3 diff, float scale, float distance) {
        return FluidCanisterModel.transportKey(blockEntity.animatedPackage, (Item) item);
    }
}
