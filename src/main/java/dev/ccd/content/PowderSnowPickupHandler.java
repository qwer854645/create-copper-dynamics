package dev.ccd.content;

import dev.ccd.CCDFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Temporary drain-only handler; extraction delegates the actual removal to vanilla BucketPickup. */
public final class PowderSnowPickupHandler implements IFluidHandler {
    private final Level level;
    private final BlockPos pos;
    public PowderSnowPickupHandler(Level level, BlockPos pos) { this.level = level; this.pos = pos.immutable(); }
    @Override public int getTanks() { return 1; }
    @Override public FluidStack getFluidInTank(int tank) {
        return tank == 0 && level.getBlockState(pos).is(Blocks.POWDER_SNOW)
            ? new FluidStack(CCDFluids.POWDER_SNOW.get(), FluidType.BUCKET_VOLUME) : FluidStack.EMPTY;
    }
    @Override public int getTankCapacity(int tank) { return tank == 0 ? FluidType.BUCKET_VOLUME : 0; }
    @Override public boolean isFluidValid(int tank, FluidStack stack) { return false; }
    @Override public int fill(FluidStack stack, FluidAction action) { return 0; }
    @Override public FluidStack drain(FluidStack resource, FluidAction action) {
        return FluidStack.isSameFluidSameComponents(getFluidInTank(0), resource) ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
    }
    @Override public FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack result = getFluidInTank(0);
        if (result.isEmpty() || maxDrain < FluidType.BUCKET_VOLUME) return FluidStack.EMPTY;
        if (action.execute()) {
            var state = level.getBlockState(pos);
            ItemStack bucket = ((BucketPickup) state.getBlock()).pickupBlock(null, level, pos, state);
            if (bucket.isEmpty()) return FluidStack.EMPTY;
        }
        return result;
    }
}
