package dev.ccd.content;

import com.simibubi.create.content.logistics.packager.repackager.RepackagerBlockEntity;
import dev.ccd.CopperDynamics;
import dev.ccd.logistics.CompositeRepackageHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class FluidRepackagerBlockEntity extends RepackagerBlockEntity {
    @Override protected void attemptToRepackage(net.neoforged.neoforge.items.IItemHandler inventory) {
        if(!dev.ccd.CCDFeatures.enabled(getBlockState()) || !dev.ccd.CCDFeatures.enabled("composite_package"))return;
        super.attemptToRepackage(inventory);
    }
    public FluidRepackagerBlockEntity(BlockPos pos,BlockState state) {
        super(CopperDynamics.FLUID_REPACKAGER_BE.get(),pos,state);
        repackageHelper=new CompositeRepackageHelper(this);
    }
    @Override public void tick() {
        if (!dev.ccd.CCDFeatures.enabled(getBlockState())) return;
        super.tick();
    }
}
