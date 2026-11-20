package dev.ccd.mixin;

import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import dev.ccd.content.TurningBoosterPumpBlock;
import dev.ccd.content.TurningBoosterPumpBlockEntity;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PumpBlockEntity.class)
abstract class TurningPumpPortsMixin {
    // Only these two calls mean "the other pump port". Opposites elsewhere describe pipe faces.
    @Redirect(method = {"updatePressureChange", "lambda$tick$0"}, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/core/Direction;getOpposite()Lnet/minecraft/core/Direction;"))
    private Direction ccd$backPort(Direction front) {
        return (Object) this instanceof TurningBoosterPumpBlockEntity pump
            ? TurningBoosterPumpBlock.branch(pump.getBlockState()) : front.getOpposite();
    }
    @Inject(method = "hasReachedValidEndpoint", at = @At("HEAD"), cancellable = true)
    private void ccd$turningPumpEndpoint(LevelAccessor world, BlockFace face, boolean pull, CallbackInfoReturnable<Boolean> cir) {
        if (world.getBlockEntity(face.getConnectedPos()) instanceof TurningBoosterPumpBlockEntity pump) {
            Direction side = face.getOppositeFace();
            cir.setReturnValue(!pump.isSideAccessible(side)
                || pump.isPullingOnSide(side == pump.getBlockState().getValue(TurningBoosterPumpBlock.FACING)) != pull);
        }
    }
}
