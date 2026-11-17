package dev.ccd.mixin;

import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlockEntity;
import net.createmod.catnip.animation.LerpedFloat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The native actuator field is package-private; expose it without replacing any valve logic. */
@Mixin(FluidValveBlockEntity.class)
public interface FluidValvePointerAccessor {
    @Accessor("pointer") LerpedFloat ccd$getPointer();
}
