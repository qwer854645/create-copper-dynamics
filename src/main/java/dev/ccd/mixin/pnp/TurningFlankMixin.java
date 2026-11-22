package dev.ccd.mixin.pnp;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import de.devin.pipesnphysics.engine.graph.Node;
import dev.ccd.compat.pnp.PnPCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo @Mixin(targets="de.devin.pipesnphysics.engine.FluidPass",remap=false)
abstract class TurningFlankMixin {
    @Shadow private Level level;
    @WrapOperation(method="assembleBranch",at=@At(value="INVOKE",target="Lnet/minecraft/core/Direction;getOpposite()Lnet/minecraft/core/Direction;"))
    private Direction ccd$pull(Direction push,Operation<Direction> original,@Local(name="pumpNode") Node node) {
        return PnPCompat.pull(level,node.pos(),push);
    }
}
