package dev.ccd.mixin.pnp;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.devin.pipesnphysics.engine.graph.Graph;
import de.devin.pipesnphysics.engine.graph.Edge;
import dev.ccd.content.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Solver flank classification only; graph edges and actual cell positions remain physical. */
@Pseudo @Mixin(targets="de.devin.pipesnphysics.engine.FluidPass",remap=false)
abstract class OmniSolverPortsMixin {
    @Shadow private Level level;
    @WrapOperation(method="assembleBranch",at=@At(value="INVOKE",target="Lde/devin/pipesnphysics/engine/graph/PipeGeometry;adjacentCell(Lde/devin/pipesnphysics/engine/graph/Graph;Lde/devin/pipesnphysics/engine/graph/Edge;I)Lnet/minecraft/core/BlockPos;"))
    private BlockPos ccd$flank(Graph graph,Edge edge,int index,Operation<BlockPos> original){
        var actual=original.call(graph,edge,index);var node=graph.node(index);
        if(node.pumpFacing()==null||!(level.getBlockEntity(node.pos()) instanceof OmniPumpBlockEntity))return actual;
        var side=net.minecraft.core.Direction.getNearest(actual.getX()-node.pos().getX(),actual.getY()-node.pos().getY(),actual.getZ()-node.pos().getZ());
        int mode=OmniPumpBlock.mode(level.getBlockState(node.pos()),side);
        return mode==OmniPumpBlock.OUTPUT?node.pushCell():mode==OmniPumpBlock.INPUT?node.pos().relative(node.pumpFacing().getOpposite()):actual;
    }
}
