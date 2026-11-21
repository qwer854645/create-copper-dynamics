package dev.ccd.compat;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import dev.ccd.mixin.BlazeBurnerFuelInvoker;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** A full bucket per native special-fuel insertion; simulation never changes burner state. */
public final class SuperLavaFuel implements IFluidHandler {
    private final BlazeBurnerBlockEntity burner;
    public SuperLavaFuel(BlazeBurnerBlockEntity burner){this.burner=burner;}
    private ProcessingMedia.Medium medium(){return ProcessingMedia.ALL.getFirst();}
    @Override public int getTanks(){return 1;}
    @Override public FluidStack getFluidInTank(int tank){int amount=burner.getPersistentData().getInt("ccdSuperLava");return amount>0?new FluidStack(medium().source.get(),amount):FluidStack.EMPTY;}
    @Override public int getTankCapacity(int tank){return 1000;}
    @Override public boolean isFluidValid(int tank,FluidStack fluid){return !fluid.isEmpty()&&fluid.getFluid().isSame(medium().source.get());}
    @Override public int fill(FluidStack fluid,FluidAction action){
        if(burner.isRemoved()||!isFluidValid(0,fluid))return 0;
        var fuel=(BlazeBurnerFuelInvoker)burner;
        if(!fuel.ccd$tryFuel(new ItemStack(medium().bucket.get()),false,true))return 0;
        int stored=Math.clamp(burner.getPersistentData().getInt("ccdSuperLava"),0,999);
        int accepted=Math.min(fluid.getAmount(),1000-stored);
        if(action.execute()){
            int total=stored+accepted;
            if(total==1000&&fuel.ccd$tryFuel(new ItemStack(medium().bucket.get()),false,false))total=0;
            burner.getPersistentData().putInt("ccdSuperLava",total);burner.setChanged();burner.notifyUpdate();
        }
        return accepted;
    }
    @Override public FluidStack drain(int amount,FluidAction action){return FluidStack.EMPTY;}
    @Override public FluidStack drain(FluidStack fluid,FluidAction action){return FluidStack.EMPTY;}
}
