package dev.ccd.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ccd.logistics.DeviceMeasurements;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({dev.ccd.content.OmniPumpBlockEntity.class,dev.ccd.content.FaucetBlockEntity.class,
    dev.ccd.content.FluidPackagerBlockEntity.class,dev.ccd.content.TankBlazeBurnerBlockEntity.class})
abstract class DeviceMeasurementMixin {
    @WrapMethod(method="tick")
    private void ccd$measure(Operation<Void> original){
        if(!DeviceMeasurements.ENABLED){original.call();return;}
        var be=(net.minecraft.world.level.block.entity.BlockEntity)(Object)this;
        if(be.getLevel()==null||be.getLevel().isClientSide()||!DeviceMeasurements.watching(be)){original.call();return;}
        long start=System.nanoTime();
        try{original.call();}finally{DeviceMeasurements.tick(getClass().getSimpleName(),System.nanoTime()-start);}
    }
}
