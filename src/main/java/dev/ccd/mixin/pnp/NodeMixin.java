package dev.ccd.mixin.pnp;

import dev.ccd.compat.pnp.TurningNode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo @Mixin(targets="de.devin.pipesnphysics.engine.graph.Node",remap=false)
abstract class NodeMixin implements TurningNode {
    @Unique private BlockPos ccd$pullCell;
    public void ccd$setPullCell(BlockPos pos){ccd$pullCell=pos;}
    @Inject(method="pullCell",at=@At("HEAD"),cancellable=true)
    private void ccd$pull(CallbackInfoReturnable<BlockPos> cir){if(ccd$pullCell!=null)cir.setReturnValue(ccd$pullCell);}
}
