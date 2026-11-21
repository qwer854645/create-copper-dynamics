package dev.ccd.mixin;

import com.simibubi.create.content.fluids.PipeConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PipeConnection.class)
public interface PipeConnectionFlowAccessor {
    @Accessor("flow") java.util.Optional<PipeConnection.Flow> ccd$getFlow();
}
