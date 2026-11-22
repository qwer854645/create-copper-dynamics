package dev.ccd.mixin.pnp;

import dev.ccd.compat.pnp.PnPCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo @Mixin(targets="de.devin.pipesnphysics.engine.boundary.OpenEndPipes",remap=false)
abstract class OpenEndMixin {
    @Inject(method="handler",at=@At("RETURN"),cancellable=true)
    private static void ccd$containers(Level level,BlockPos pos,Direction face,CallbackInfoReturnable<IFluidHandler> cir) {
        var handler=cir.getReturnValue();if(handler==null||!PnPCompat.CONTAINERS_ONLY.get())return;
        cir.setReturnValue(new IFluidHandler(){
            public int getTanks(){return handler.getTanks();}
            public FluidStack getFluidInTank(int tank){return handler.getFluidInTank(tank);}
            public int getTankCapacity(int tank){return handler.getTankCapacity(tank);}
            public boolean isFluidValid(int tank,FluidStack fluid){return false;}
            public int fill(FluidStack fluid,FluidAction action){return 0;}
            public FluidStack drain(int amount,FluidAction action){return handler.drain(amount,action);}
            public FluidStack drain(FluidStack fluid,FluidAction action){return handler.drain(fluid,action);}
        });
    }
}
