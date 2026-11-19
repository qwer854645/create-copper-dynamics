package dev.ccd.content;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.repackager.RepackagerBlock;
import dev.ccd.CopperDynamics;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Dedicated fluid-aware machine; placement and redstone operation remain native. */
public final class FluidRepackagerBlock extends RepackagerBlock {
    public FluidRepackagerBlock(Properties properties) { super(properties); }
    @Override public BlockEntityType<? extends PackagerBlockEntity> getBlockEntityType() {
        return CopperDynamics.FLUID_REPACKAGER_BE.get();
    }
}
