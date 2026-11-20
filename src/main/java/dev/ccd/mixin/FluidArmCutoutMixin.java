package dev.ccd.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmRenderer;
import dev.ccd.content.FluidArmBlockEntity;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ArmRenderer.class)
abstract class FluidArmCutoutMixin {
    @ModifyExpressionValue(method="renderSafe(Lcom/simibubi/create/content/kinetics/mechanicalArm/ArmBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/RenderType;solid()Lnet/minecraft/client/renderer/RenderType;"))
    private RenderType ccd$transparentSyringe(RenderType original,@Local(argsOnly=true) ArmBlockEntity entity) {
        return entity instanceof FluidArmBlockEntity?RenderType.cutout():original;
    }
}
