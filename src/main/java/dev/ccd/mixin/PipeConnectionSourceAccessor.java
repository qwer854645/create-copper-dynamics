package dev.ccd.mixin;

import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.content.fluids.FlowSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PipeConnection.class)
public interface PipeConnectionSourceAccessor {
    @Accessor("source") void ccd$source(java.util.Optional<FlowSource> source);
}
