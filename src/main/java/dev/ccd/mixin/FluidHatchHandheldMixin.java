package dev.ccd.mixin;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.fluid.FluidHelper;
import dev.ccd.content.FluidHatchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Feed Create's original handheld exchange directly, without exposing a pipe capability. */
@Mixin(FluidHelper.class)
abstract class FluidHatchHandheldMixin {
    @SuppressWarnings("unchecked")
    @Redirect(method = {"tryEmptyItemIntoBE", "tryFillItemFromBE"}, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;getCapability(Lnet/neoforged/neoforge/capabilities/BlockCapability;Lnet/minecraft/core/BlockPos;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object ccd$handheldInterface(Level level, BlockCapability<?, ?> capability, BlockPos pos, Object context,
        Level exchangeLevel, Player player, InteractionHand hand, ItemStack stack, SmartBlockEntity entity) {
        if (entity instanceof FluidHatchBlockEntity hatch) return hatch.interactionHandler(null);
        return level.getCapability((BlockCapability<Object, Object>) capability, pos, context);
    }
}
