package dev.ccd.mixin;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PartialModel.class)
public interface CanisterPartialModelAccessor {
    @Invoker("<init>") static PartialModel ccd$create(ResourceLocation location) { throw new AssertionError(); }
    @Accessor("bakedModel") void ccd$setModel(BakedModel model);
}
