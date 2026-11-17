package dev.ccd.mixin;

import java.util.List;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Access native package-private state while retaining the native scheduler and animation. */
@Mixin(ArmBlockEntity.class)
public interface FluidArmAccessor {
    @Accessor("inputs") List<ArmInteractionPoint> ccd$inputs();
    @Accessor("outputs") List<ArmInteractionPoint> ccd$outputs();
    @Accessor("interactionPointTag") ListTag ccd$pointTag();
    @Accessor("interactionPointTag") void ccd$pointTag(ListTag tag);
    @Accessor("updateInteractionPoints") boolean ccd$pointsDirty();
    @Accessor("updateInteractionPoints") void ccd$pointsDirty(boolean dirty);
    @Accessor("heldItem") ItemStack ccd$held();
    @Accessor("heldItem") void ccd$held(ItemStack stack);
    @Accessor("goggles") boolean ccd$goggles();
    @Accessor("phase") ArmBlockEntity.Phase ccd$phase();
    @Accessor("phase") void ccd$phase(ArmBlockEntity.Phase phase);
    @Accessor("chasedPointIndex") void ccd$target(int index);
    @Accessor("chasedPointProgress") void ccd$progress(float progress);
}
