package dev.ccd.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(com.simibubi.create.content.fluids.FluidPropagator.class)
abstract class EndpointPressureUpdateMixin {
    @Redirect(method="propagateChangedPipe",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/LevelAccessor;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private static BlockEntity ccd$endpointChanged(LevelAccessor level,BlockPos pos){
        var be=level.getBlockEntity(pos);
        if(!dev.ccd.logistics.NativeEndpointPump.UPDATING.get()){
            if(be instanceof dev.ccd.content.BufferPumpBlockEntity pump)pump.invalidatePressure();
            if(be instanceof dev.ccd.content.OmniPumpBlockEntity pump)pump.invalidatePressure();
            if(be instanceof dev.ccd.content.CreativeDeliveryTankBlockEntity tank)tank.invalidatePressure();
        }
        return be;
    }
}
