package dev.ccd.mixin;

import com.simibubi.create.content.logistics.box.PackageEntity;
import dev.ccd.content.FluidPackageItem;
import dev.ccd.content.CompositePackageItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PackageEntity.class)
abstract class PackageEntityMixin {
    @Shadow public ItemStack box;

    @Inject(method = "onInsideBlock", at = @At("HEAD"), cancellable = true)
    private void ccd$sealedParcelResistsWater(BlockState state, CallbackInfo ci) {
        if (CompositePackageItem.isSealed(box) && (state.is(Blocks.WATER)
            || (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)))) {
            ci.cancel();
        }
    }

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void ccd$recoverSealedParcel(ServerLevel level, DamageSource source, CallbackInfo ci) {
        if (!CompositePackageItem.isSealed(box)) return;
        PackageEntity entity = (PackageEntity) (Object) this;
        // Do not resurrect cargo destroyed by fire or explosions. Other ordinary attacks
        // produce a plain recovery item, not another fragile PackageEntity in a loop.
        if (!source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
            && !source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
            ItemStack saved = box.copyWithCount(1);
            saved.set(dev.ccd.CopperDynamics.RECOVERED.get(), true);
            ItemEntity drop = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), saved);
            drop.getPersistentData().putBoolean("CCDRecoveryDrop",true);
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        }
        box = ItemStack.EMPTY;
        ci.cancel();
    }
}
