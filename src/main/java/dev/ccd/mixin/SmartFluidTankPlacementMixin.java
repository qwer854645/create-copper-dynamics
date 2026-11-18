package dev.ccd.mixin;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankItem;
import dev.ccd.CopperDynamics;
import dev.ccd.content.SmartFluidTankBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Reuse the native 2x2/3x3 layer placement, changing only CCD's tank/type checks. */
@Mixin(FluidTankItem.class)
abstract class SmartFluidTankPlacementMixin {
    private boolean ccd$isSmartTankItem() {
        return ((BlockItem) (Object) this).getBlock() instanceof SmartFluidTankBlock;
    }

    @Redirect(method = "tryMultiPlace", at = @At(value = "INVOKE",
        target = "Lcom/simibubi/create/content/fluids/tank/FluidTankBlock;isTank(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean ccd$matchTankKind(BlockState state) {
        return ccd$isSmartTankItem() ? state.is(CopperDynamics.SMART_FLUID_TANK.get()) : FluidTankBlock.isTank(state);
    }

    @Redirect(method = "tryMultiPlace", at = @At(value = "INVOKE",
        target = "Lcom/simibubi/create/api/connectivity/ConnectivityHandler;partAt(Lnet/minecraft/world/level/block/entity/BlockEntityType;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private BlockEntity ccd$findController(BlockEntityType<?> type, BlockGetter level, BlockPos pos) {
        return ccd$isSmartTankItem()
            ? ConnectivityHandler.partAt(CopperDynamics.SMART_FLUID_TANK_BE.get(), level, pos)
            : ConnectivityHandler.partAt(type, level, pos);
    }
}
