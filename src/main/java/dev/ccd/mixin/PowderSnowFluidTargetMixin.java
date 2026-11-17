package dev.ccd.mixin;

import com.simibubi.create.content.fluids.pipes.VanillaFluidTargets;
import dev.ccd.content.PowderSnowPickupHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Create's vanilla world targets are hardcoded and its ordinary handler source requires a block entity. */
@Mixin(VanillaFluidTargets.class)
public abstract class PowderSnowFluidTargetMixin {
    private static boolean ccd$isSnow(BlockState state) { return state.is(Blocks.POWDER_SNOW) || state.is(Blocks.POWDER_SNOW_CAULDRON); }
    @Inject(method = "canProvideFluidWithoutCapability", at = @At("HEAD"), cancellable = true)
    private static void ccd$recognizeSnow(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (ccd$isSnow(state)) cir.setReturnValue(true);
    }
    @Inject(method = "drainBlock", at = @At("HEAD"), cancellable = true)
    private static void ccd$drainSnow(Level level, BlockPos pos, BlockState state, boolean simulate, CallbackInfoReturnable<FluidStack> cir) {
        if (!ccd$isSnow(state)) return;
        var handler = state.is(Blocks.POWDER_SNOW) ? new PowderSnowPickupHandler(level, pos)
            : level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        cir.setReturnValue(handler == null ? FluidStack.EMPTY : handler.drain(1000, simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE));
    }
}
