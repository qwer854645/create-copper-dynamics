package dev.ccd.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.ccd.client.FluidArmModels;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmVisual;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;

/** Constructor substitutions occur after the native visual has initialized its block state. */
@Mixin(ArmVisual.class)
abstract class FluidArmVisualModelsMixin extends SingleAxisRotatingVisual<ArmBlockEntity> {
    protected FluidArmVisualModelsMixin(VisualizationContext context, ArmBlockEntity entity, float pt, Model model) {
        super(context, entity, pt, model);
    }
    @ModifyExpressionValue(method = {"<init>", "update"}, at = {
        @At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;ARM_LOWER_BODY:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"),
        @At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;ARM_UPPER_BODY:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"),
        @At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;ARM_CLAW_BASE:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"),
        @At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;ARM_CLAW_BASE_GOGGLES:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"),
        @At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;ARM_CLAW_GRIP_UPPER:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"),
        @At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;ARM_CLAW_GRIP_LOWER:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;")
    })
    private PartialModel ccd$copperModels(PartialModel original) {
        return FluidArmModels.select(original, blockState);
    }
}
