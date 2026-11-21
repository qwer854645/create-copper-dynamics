package dev.ccd.mixin;

import com.simibubi.create.content.fluids.pump.PumpBlock;
import dev.ccd.content.PumpPlacement;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PumpBlock.class)
abstract class PumpPlacementMixin {
    @Inject(method="getStateForPlacement",at=@At("RETURN"),cancellable=true)
    private void ccd$alignMouth(BlockPlaceContext context,CallbackInfoReturnable<BlockState> cir){
        cir.setReturnValue(PumpPlacement.align(context,cir.getReturnValue()));
    }
}
