package dev.ccd.mixin;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import dev.ccd.content.FluidGaugeBehaviour;
import dev.ccd.content.FluidPackageItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** The native restock path caps a request at nine item stacks. Only CCD's gauge
 * changes that cap to one fluid package; distribution and promises stay native. */
@Mixin(FactoryPanelBehaviour.class)
abstract class FluidGaugeRestockMixin {
    @ModifyConstant(method = "tryRestock", constant = @Constant(intValue = 9))
    private int ccd$fluidPackageLimit(int original) {
        return (Object) this instanceof FluidGaugeBehaviour ? FluidPackageItem.CAPACITY : original;
    }
}
