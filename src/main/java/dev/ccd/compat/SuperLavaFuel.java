package dev.ccd.compat;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import dev.ccd.mixin.BlazeBurnerFuelInvoker;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** A full bucket per native fuel insertion; simulation never changes burner state. */
public final class SuperLavaFuel implements IFluidHandler {
    private final BlazeBurnerBlockEntity burner;
    public SuperLavaFuel(BlazeBurnerBlockEntity burner){this.burner=burner;}
    private ProcessingMedia.Medium medium(){return ProcessingMedia.ALL.getFirst();}
    @Override public int getTanks(){return 1;}
    @Override public FluidStack getFluidInTank(int tank){
        int amount=burner.getPersistentData().getInt("ccdSuperLava");
        return amount>0?new FluidStack(medium().source.get(),amount):FluidStack.parseOptional(burner.getLevel().registryAccess(),burner.getPersistentData().getCompound("ccdBurnerFluid"));
    }
    @Override public int getTankCapacity(int tank){return 1000;}
    private ItemStack fuelItem(FluidStack fluid){
        return fluid.isEmpty()?ItemStack.EMPTY:new ItemStack(fluid.getFluid().getBucket());
    }
    @Override public boolean isFluidValid(int tank,FluidStack fluid){
        if(fluid.isEmpty())return false;
        var item=fuelItem(fluid);
        return !item.isEmpty()&&((BlazeBurnerFuelInvoker)burner).ccd$tryFuel(item,false,true);
    }
    @Override public int fill(FluidStack fluid,FluidAction action){
        if(burner.isRemoved()||!isFluidValid(0,fluid))return 0;
        var fuel=(BlazeBurnerFuelInvoker)burner;
        var item=fuelItem(fluid);
        if(!fuel.ccd$tryFuel(item,false,true))return 0;
        var buffered=getFluidInTank(0);
        if(!buffered.isEmpty()&&!FluidStack.isSameFluidSameComponents(buffered,fluid))return 0;
        int stored=Math.clamp(buffered.getAmount(),0,999);
        int accepted=Math.min(fluid.getAmount(),1000-stored);
        if(action.execute()){
            int total=stored+accepted;
            if(total==1000&&fuel.ccd$tryFuel(item,false,false))total=0;
            boolean special=fluid.getFluid().isSame(medium().source.get());
            burner.getPersistentData().putInt("ccdSuperLava",special?total:0);
            burner.getPersistentData().put("ccdBurnerFluid",(special||total==0?FluidStack.EMPTY:fluid.copyWithAmount(total)).saveOptional(burner.getLevel().registryAccess()));
            burner.setChanged();burner.notifyUpdate();
        }
        return accepted;
    }
    @Override public FluidStack drain(int amount,FluidAction action){return FluidStack.EMPTY;}
    @Override public FluidStack drain(FluidStack fluid,FluidAction action){return FluidStack.EMPTY;}
}
