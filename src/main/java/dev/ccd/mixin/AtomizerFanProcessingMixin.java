package dev.ccd.mixin;

import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessing;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import dev.ccd.atomizing.AtomizerProcessing;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supply custom timers without replacing Create's stack conversion, processing loop or item entity handling. */
@Mixin(FanProcessing.class)
public abstract class AtomizerFanProcessingMixin {
    @Inject(method = "canProcess(Lnet/minecraft/world/entity/item/ItemEntity;Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;)Z", at = @At("HEAD"))
    private static void ccd$prepareEntity(ItemEntity item, FanProcessingType type, CallbackInfoReturnable<Boolean> cir) {
        AtomizerProcessing.prepare(item, type);
    }
    @Inject(method = "applyProcessing(Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;Lnet/minecraft/world/level/Level;Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;)Lcom/simibubi/create/content/kinetics/belt/behaviour/TransportedItemStackHandlerBehaviour$TransportedResult;", at = @At("HEAD"))
    private static void ccd$prepareTransported(TransportedItemStack item, Level level, FanProcessingType type, CallbackInfoReturnable<TransportedResult> cir) {
        AtomizerProcessing.prepare(item, type);
    }
}
