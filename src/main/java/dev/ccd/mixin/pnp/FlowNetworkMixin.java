package dev.ccd.mixin.pnp;

import de.devin.pipesnphysics.engine.flow.FlowLedger;
import de.devin.pipesnphysics.engine.graph.Graph;
import dev.ccd.content.FlowMeterBlockEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo @Mixin(targets="de.devin.pipesnphysics.engine.flow.FlowNetwork",remap=false)
abstract class FlowNetworkMixin {
    @Shadow private Level level;
    @Shadow private Graph graph;
    @Inject(method="stampPumps",at=@At("TAIL"))
    private void ccd$measure(FlowLedger ledger,CallbackInfo ci) {
        for(var edge:graph.edges()) {
            int moved=ledger.edgeMovedMb()[edge.index()];if(moved<=0)continue;
            for(var pos:edge.pipes())if(level.getBlockEntity(pos) instanceof FlowMeterBlockEntity meter)meter.recordTransfer(moved);
        }
    }
}
