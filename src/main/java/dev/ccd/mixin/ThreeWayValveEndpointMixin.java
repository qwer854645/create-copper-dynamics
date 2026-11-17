package dev.ccd.mixin;

import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import dev.ccd.content.ThreeWayValveBlock;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The native pressure walk does not check the next pipe's reciprocal port before entering it.
 * Mark a closed CCD port as a terminal graph face. It has no fluid handler/open end, so it
 * cannot accept or supply liquid, and its pressure cannot leak into the selected route. */
@Mixin(PumpBlockEntity.class)
abstract class ThreeWayValveEndpointMixin {
    @Inject(method = "hasReachedValidEndpoint", at = @At("HEAD"), cancellable = true)
    private void ccd$closedThreeWayPort(LevelAccessor world, BlockFace face, boolean pull,
        CallbackInfoReturnable<Boolean> cir) {
        var state = world.getBlockState(face.getConnectedPos());
        if (state.getBlock() instanceof ThreeWayValveBlock
            && !ThreeWayValveBlock.open(state, face.getOppositeFace())) cir.setReturnValue(true);
    }
}
