package dev.ccd.mixin;

import dev.ccd.content.VariableFlowPumpBlock;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The ordinary small/large helpers run as well as the integrated-cog helper. */
@Mixin(targets={"com.simibubi.create.content.kinetics.simpleRelays.CogwheelBlockItem$SmallCogHelper",
    "com.simibubi.create.content.kinetics.simpleRelays.CogwheelBlockItem$LargeCogHelper"})
abstract class FrequencyPumpCogPreviewMixin {
    @Inject(method="getOffset",at=@At("HEAD"),cancellable=true)
    private void ccd$rejectCoveredFaces(Player player,Level level,BlockState state,BlockPos pos,
        BlockHitResult hit,CallbackInfoReturnable<PlacementOffset> cir){
        if(state.getBlock() instanceof VariableFlowPumpBlock block && block.isSmallCog()
            && hit.getDirection().getAxis()!=VariableFlowPumpBlock.gearContactAxis(state))
            cir.setReturnValue(PlacementOffset.fail());
    }
}
