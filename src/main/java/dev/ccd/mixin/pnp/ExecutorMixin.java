package dev.ccd.mixin.pnp;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import de.devin.pipesnphysics.engine.PipeFlowExecutor;
import de.devin.pipesnphysics.engine.Solution;
import de.devin.pipesnphysics.engine.graph.Graph;
import dev.ccd.compat.pnp.PnPCompat;
import dev.ccd.content.CreativeDeliveryTankBlockEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo @Mixin(targets="de.devin.pipesnphysics.engine.PipeFlowExecutor",remap=false)
abstract class ExecutorMixin {
    @WrapMethod(method="run")
    private static PipeFlowExecutor.Actuals ccd$outputPolicy(Level level,Graph graph,Solution solution,Operation<PipeFlowExecutor.Actuals> original) {
        boolean old=PnPCompat.CONTAINERS_ONLY.get();
        boolean containers=graph.nodes().stream().anyMatch(node->level.getBlockEntity(node.pos()) instanceof CreativeDeliveryTankBlockEntity tank&&tank.worldOutput.getValue()==0);
        PnPCompat.CONTAINERS_ONLY.set(old||containers);
        try{return original.call(level,graph,solution);}finally{PnPCompat.CONTAINERS_ONLY.set(old);}
    }
}
