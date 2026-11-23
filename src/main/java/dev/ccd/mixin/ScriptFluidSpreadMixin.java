package dev.ccd.mixin;

import dev.ccd.api.FluidWorldScripts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Lava overrides spreadTo and may react before calling super; intercept both implementations.
@Mixin({net.minecraft.world.level.material.FlowingFluid.class,net.minecraft.world.level.material.LavaFluid.class})
abstract class ScriptFluidSpreadMixin {
    @Inject(method="spreadTo",at=@At("HEAD"),cancellable=true)
    private void ccd$scriptDownward(LevelAccessor level,BlockPos pos,BlockState state,Direction direction,FluidState incoming,CallbackInfo ci){
        if(FluidWorldScripts.cancels(level,pos.relative(direction.getOpposite()),pos,incoming)){ci.cancel();return;}
        if(FluidWorldScripts.downward(level,pos,direction,incoming))ci.cancel();
    }
}
