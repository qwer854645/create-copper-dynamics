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
        if(level.getBlockEntity(pos) instanceof dev.ccd.content.OmniPumpBlockEntity){cir.setReturnValue(true);return;}
        if(PnPCompat.endpoint(level.getBlockEntity(pos)))cir.setReturnValue(false);
    }
    @Inject(method={"strength","publishedStrength"},at=@At("HEAD"),cancellable=true)
    private static void ccd$strength(Level level,BlockPos pos,CallbackInfoReturnable<Double> cir) {
        var be=level.getBlockEntity(pos);
        if(be instanceof dev.ccd.content.OmniPumpBlockEntity omni){cir.setReturnValue(omni.powered()&&omni.hasPorts(dev.ccd.content.OmniPumpBlock.INPUT)&&omni.hasPorts(dev.ccd.content.OmniPumpBlock.OUTPUT)?(double)Math.abs(omni.getSpeed()):0d);return;}
        if(PnPCompat.pump(be))cir.setReturnValue(PnPCompat.strength((PumpBlockEntity)be));
    }
    @Inject(method="pushSide",at=@At("HEAD"),cancellable=true)
    private static void ccd$direction(Level level,BlockPos pos,BlockState state,CallbackInfoReturnable<Direction> cir) {
        var be=level.getBlockEntity(pos);
        if(be instanceof dev.ccd.content.OmniPumpBlockEntity){for(var side:Direction.values())if(dev.ccd.content.OmniPumpBlock.mode(state,side)==dev.ccd.content.OmniPumpBlock.OUTPUT){cir.setReturnValue(side);return;}cir.setReturnValue(null);return;}
        if(PnPCompat.pump(be))cir.setReturnValue(PnPCompat.push((PumpBlockEntity)be));
    }
}
