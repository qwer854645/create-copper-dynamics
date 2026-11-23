package dev.ccd.mixin.pnp;

import de.devin.pipesnphysics.engine.boundary.BoundaryColumn;
import de.devin.pipesnphysics.engine.graph.Node;
import de.devin.pipesnphysics.PipesNPhysicsConfig;
import dev.ccd.content.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo @Mixin(targets="de.devin.pipesnphysics.engine.boundary.BoundaryColumn",remap=false)
abstract class BoundaryColumnMixin {
    @Shadow @Final @Mutable private double baseY;
    @Inject(method="resolve",at=@At("RETURN"))
    private static void ccd$pressure(Level level,Node node,CallbackInfoReturnable<BoundaryColumn> cir) {
        var column=cir.getReturnValue();if(column==null)return;
        var be=level.getBlockEntity(node.pos());double rpm=0;boolean endpoint=false;
        if(be!=null&&!dev.ccd.CCDFeatures.enabled(be.getBlockState()))return;
        if(be instanceof BufferPumpBlockEntity pump) {
            endpoint=true;
            if(node.accessFace()==pump.getBlockState().getValue(BufferPumpBlock.FACING)&&!pump.isOverStressed())rpm=Math.abs(pump.getSpeed());
        } else if(be instanceof CreativeDeliveryTankBlockEntity) {endpoint=true;rpm=512;}
        // Shift the hydraulic datum and its capacity ceiling together. Finite storage remains
        // finite; only the creative relay is an infinite boundary. Actual volume stays in the cap.
        if(endpoint)((BoundaryColumnMixin)(Object)column).baseY+=rpm*PipesNPhysicsConfig.PUMP_HEAD_PER_RPM.get();
    }
}
