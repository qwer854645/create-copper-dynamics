package dev.ccd.mixin;

import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only disabled CCD fluids are excluded; enabled/native catalysts use the original lookup. */
@Mixin(FanProcessingType.class)
public interface DisabledFluidCatalystMixin {
    @Inject(method="getAt",at=@At("HEAD"),cancellable=true)
    private static void ccd$disabledMedium(Level level, BlockPos pos, CallbackInfoReturnable<FanProcessingType> cir) {
        var fluid=level.getFluidState(pos);
        if(!fluid.isEmpty()&&!dev.ccd.CCDFeatures.enabled(fluid.getType()))cir.setReturnValue(null);
    }
}
