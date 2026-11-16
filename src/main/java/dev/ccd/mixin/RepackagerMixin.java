package dev.ccd.mixin;

import com.simibubi.create.content.logistics.packager.repackager.PackageRepackageHelper;
import com.simibubi.create.content.logistics.packager.repackager.RepackagerBlockEntity;
import dev.ccd.logistics.CompositeRepackageHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep the native machine, scan loop, animations, inventory capability and queue. */
@Mixin(RepackagerBlockEntity.class)
abstract class RepackagerMixin {
    @Shadow public PackageRepackageHelper repackageHelper;
    @Inject(method = "<init>", at = @At("TAIL"))
    private void ccd$extendNativeHelper(BlockEntityType<?> type, BlockPos pos, BlockState state, CallbackInfo ci) {
        repackageHelper = new CompositeRepackageHelper((RepackagerBlockEntity) (Object) this);
    }
}
