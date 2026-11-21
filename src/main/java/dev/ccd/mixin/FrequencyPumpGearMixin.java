package dev.ccd.mixin;

import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.ccd.content.VariableFlowPumpBlock;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RotationPropagator.class)
abstract class FrequencyPumpGearMixin {
    @Inject(method="getRotationSpeedModifier",at=@At("HEAD"),cancellable=true)
    private static void ccd$blockPipeFaceGears(KineticBlockEntity from,KineticBlockEntity to,CallbackInfoReturnable<Float> cir){
        for(var pump:new KineticBlockEntity[]{from,to}){
            var state=pump.getBlockState();
            if(!(state.getBlock() instanceof VariableFlowPumpBlock block)||!block.isSmallCog())continue;
            var other=pump==from?to:from;
            var diff=other.getBlockPos().subtract(pump.getBlockPos());
            // Only top/bottom adjacent gears reach the exposed wheel; pipe mouths cannot mesh.
            if(diff.getX()!=0||diff.getZ()!=0||Math.abs(diff.getY())!=1)cir.setReturnValue(0f);
        }
    }
}
