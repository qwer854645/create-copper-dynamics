package dev.ccd.mixin;

import dev.ccd.content.ReciprocatingFluidPumpBlock;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="com.simibubi.create.content.kinetics.simpleRelays.CogwheelBlockItem$IntegratedSmallCogHelper")
abstract class FrequencyPumpPlacementHelperMixin {
    @Inject(method="getOffset",at=@At("HEAD"),cancellable=true)
    private void ccd$exposedWheelOnly(Player player,Level level,BlockState state,BlockPos pos,BlockHitResult hit,CallbackInfoReturnable<PlacementOffset> cir){
        if(!(state.getBlock() instanceof ReciprocatingFluidPumpBlock block)||!block.isSmallCog())return;
        if(hit.getDirection().getAxis()!=Direction.Axis.Y){cir.setReturnValue(PlacementOffset.fail());return;}
        var target=pos.relative(hit.getDirection());var axis=block.getRotationAxis(state);
        if(!level.getBlockState(target).canBeReplaced()||!com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock.isValidCogwheelPosition(false,level,target,axis)){
            cir.setReturnValue(PlacementOffset.fail());return;
        }
        cir.setReturnValue(PlacementOffset.success().at(target).withTransform(placed->placed.setValue(com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock.AXIS,axis)));
    }
}
