package dev.ccd.mixin;

import dev.ccd.logistics.DeviceMeasurements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(com.simibubi.create.foundation.blockEntity.SyncedBlockEntity.class)
abstract class DeviceSyncMeasurementMixin {
    @Inject(method="sendData",at=@At("HEAD"))
    private void ccd$syncRequest(CallbackInfo ci){
        if(DeviceMeasurements.ENABLED){
            var be=(net.minecraft.world.level.block.entity.BlockEntity)(Object)this;
            if(be.getLevel()!=null&&!be.getLevel().isClientSide()&&DeviceMeasurements.watching(be))DeviceMeasurements.sync(getClass().getSimpleName());
        }
    }
}
