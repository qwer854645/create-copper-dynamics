package dev.ccd.mixin.pnp;

import de.devin.pipesnphysics.engine.graph.Graph;
import dev.ccd.compat.pnp.PnPCompat;
import dev.ccd.compat.pnp.TurningNode;
import dev.ccd.content.TurningBoosterPumpBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo @Mixin(targets="de.devin.pipesnphysics.engine.graph.GraphBuilder",remap=false)
abstract class GraphBuilderMixin {
    @Inject(method="build",at=@At("RETURN"))
    private static void ccd$turn(Level level,BlockPos pos,CallbackInfoReturnable<Graph> cir) {
        for(var node:cir.getReturnValue().pumps())if(node.pumpFacing()!=null&&level.getBlockState(node.pos()).getBlock() instanceof TurningBoosterPumpBlock)
            ((TurningNode)(Object)node).ccd$setPullCell(node.pos().relative(PnPCompat.pull(level,node.pos(),node.pumpFacing())));
    }
}
