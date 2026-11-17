package dev.ccd.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.ccd.client.FluidArmModels;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmRenderer;
import net.minecraft.world.level.block.state.BlockState;

/** Only substitute CCD material variants; native transforms and item rendering remain untouched. */
@Mixin(ArmRenderer.class)
abstract class FluidArmRendererModelsMixin {
    @ModifyExpressionValue(method = "renderArm", at = {
        @At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;ARM_LOWER_BODY:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"),
        @At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;ARM_UPPER_BODY:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"),
        @At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;ARM_CLAW_BASE:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"),
        @At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;ARM_CLAW_BASE_GOGGLES:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"),
        @At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;ARM_CLAW_GRIP_UPPER:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"),
        @At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;ARM_CLAW_GRIP_LOWER:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;")
    })
    private PartialModel ccd$copperModels(PartialModel original, @Local(argsOnly = true) BlockState state) {
        return FluidArmModels.select(original, state);
    }
}
