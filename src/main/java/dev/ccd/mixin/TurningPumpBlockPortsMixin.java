package dev.ccd.mixin;

import com.simibubi.create.content.fluids.pump.PumpBlock;
import dev.ccd.content.TurningBoosterPumpBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PumpBlock.class)
abstract class TurningPumpBlockPortsMixin {
    @Inject(method = "isOpenAt", at = @At("HEAD"), cancellable = true)
    private static void ccd$turningPorts(BlockState state, Direction side, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof TurningBoosterPumpBlock) cir.setReturnValue(TurningBoosterPumpBlock.open(state, side));
        else if(state.getBlock() instanceof dev.ccd.content.BufferPumpBlock)cir.setReturnValue(dev.ccd.content.PumpPlacement.mouth(state,side));
    }
}
