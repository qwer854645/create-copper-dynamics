package dev.ccd.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagerRenderer;
import dev.ccd.content.FluidRepackagerBlockEntity;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PackagerRenderer.class)
abstract class FluidRepackagerCutoutMixin {
    @ModifyExpressionValue(method="renderSafe(Lcom/simibubi/create/content/logistics/packager/PackagerBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/RenderType;solid()Lnet/minecraft/client/renderer/RenderType;"))
    private RenderType ccd$cutoutHatch(RenderType original,@Local(argsOnly=true) PackagerBlockEntity entity) {
        return entity instanceof FluidRepackagerBlockEntity?RenderType.cutoutMipped():original;
    }
}
