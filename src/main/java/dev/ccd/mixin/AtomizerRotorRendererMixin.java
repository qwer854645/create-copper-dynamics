package dev.ccd.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.content.kinetics.fan.EncasedFanRenderer;
import dev.ccd.content.FluidAtomizerBlockEntity;
import dev.ccd.client.AtomizerModels;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(EncasedFanRenderer.class)
abstract class AtomizerRotorRendererMixin {
    @ModifyExpressionValue(method="renderSafe(Lcom/simibubi/create/content/kinetics/fan/EncasedFanBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",at=@At(value="FIELD",target="Lcom/simibubi/create/AllPartialModels;ENCASED_FAN_INNER:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"))
    private PartialModel ccd$rotor(PartialModel original,@Local(argsOnly=true) EncasedFanBlockEntity entity) {
        return entity instanceof FluidAtomizerBlockEntity?AtomizerModels.ROTOR:original;
    }
}
