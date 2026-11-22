package dev.ccd.mixin.pnp;

import dev.ccd.compat.pnp.PnPCompat;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="de.devin.pipesnphysics.engine.pump.Pumps",remap=false)
abstract class PumpsMixin {
    @Inject(method="isPump",at=@At("HEAD"),cancellable=true)
    private static void ccd$endpoint(Level level,BlockPos pos,BlockState state,CallbackInfoReturnable<Boolean> cir) {
        if(PnPCompat.endpoint(level.getBlockEntity(pos)))cir.setReturnValue(false);
    }
    @Inject(method={"strength","publishedStrength"},at=@At("HEAD"),cancellable=true)
    private static void ccd$strength(Level level,BlockPos pos,CallbackInfoReturnable<Double> cir) {
        var be=level.getBlockEntity(pos);
        if(PnPCompat.pump(be))cir.setReturnValue(PnPCompat.strength((PumpBlockEntity)be));
    }
    @Inject(method="pushSide",at=@At("HEAD"),cancellable=true)
    private static void ccd$direction(Level level,BlockPos pos,BlockState state,CallbackInfoReturnable<Direction> cir) {
        var be=level.getBlockEntity(pos);
        if(PnPCompat.pump(be))cir.setReturnValue(PnPCompat.push((PumpBlockEntity)be));
    }
}
