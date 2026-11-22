package dev.ccd.mixin;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** CCD owns its tank/fuel bookkeeping; leave ordinary Liquid Fuel burners untouched. */
@Pseudo @Mixin(targets="com.forsteri.createliquidfuel.core.BurnerStomachHandler",remap=false)
abstract class LiquidFuelTankBurnerMixin {
    private static boolean ccd$ownsFuel(SmartBlockEntity be){
        return be instanceof dev.ccd.content.TankBlazeBurnerBlockEntity
            ||be.getLevel()!=null&&be.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,be.getBlockPos(),net.minecraft.core.Direction.DOWN) instanceof dev.ccd.compat.SuperLavaFuel;
    }
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private static void ccd$ownTick(SmartBlockEntity be,CallbackInfo ci){
        if(ccd$ownsFuel(be))ci.cancel();
    }
    @Inject(method="tryUpdateFuel",at=@At("HEAD"),cancellable=true)
    private static void ccd$nativeBucket(SmartBlockEntity be,ItemStack item,boolean force,boolean simulate,CallbackInfoReturnable<Boolean> original,CallbackInfo ci){
        if(ccd$ownsFuel(be))ci.cancel();
    }
}
