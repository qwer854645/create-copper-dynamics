package dev.ccd.content;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import com.simibubi.create.foundation.fluid.SmartFluidTank;

/** Native burner with a finite reservoir; all actual fuel acceptance is decided by Create. */
public final class TankBlazeBurnerBlockEntity extends BlazeBurnerBlockEntity implements com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation {
    public SmartFluidTank tank;
    private boolean acceptsFuel(FluidStack fluid){
            // Buffer native liquid fuels even while the burner cannot currently take more fuel.
            var bucket=new ItemStack(fluid.getFluid().getBucket());
            if(fluid.isEmpty())return false;
            if(dev.ccd.compat.LiquidFuelCompat.find(fluid.getFluid())!=null)return true;
            return !bucket.isEmpty()
                && (fluid.getFluid().isSame(net.minecraft.world.level.material.Fluids.LAVA)
                || fluid.getFluid().isSame(dev.ccd.compat.ProcessingMedia.ALL.getFirst().source.get())
                || bucket.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING)>0
                || tryUpdateFuel(bucket,false,true));
    }
    public TankBlazeBurnerBlockEntity(BlockPos pos,BlockState state){super(CopperDynamics.TANK_BURNER_BE.get(),pos,state);}
    @Override public void addBehaviours(java.util.List<com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour> behaviours){
        super.addBehaviours(behaviours);
        var storage=com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.single(this,4000);
        behaviours.add(storage);tank=storage.getPrimaryHandler();tank.setValidator(this::acceptsFuel);
    }
    @Override public void tick(){
        if (!dev.ccd.CCDFeatures.enabled(getBlockState())) return;
        super.tick();
        if(level!=null&&!level.isClientSide()&&!tank.isEmpty()){
            var fuel=dev.ccd.compat.LiquidFuelCompat.find(tank.getFluid().getFluid());
            if(fuel!=null){
                // A queued ordinary liquid must not downgrade still-burning special fuel.
                if(activeFuel==FuelType.SPECIAL&&!fuel.superheated())return;
                if(tank.getFluidAmount()>=fuel.amount()&&remainingBurnTime<=MAX_HEAT_CAPACITY-fuel.ticks()){
                    activeFuel=fuel.superheated()?FuelType.SPECIAL:FuelType.NORMAL;
                    remainingBurnTime+=fuel.ticks();
                    tank.drain(fuel.amount(),IFluidHandler.FluidAction.EXECUTE);
                    updateBlockState();
                }
                return;
            }
        }
        if(level==null||level.isClientSide()||level.getGameTime()%10!=0||tank.getFluidAmount()<1000)return;
        var fluid=tank.getFluid();
        var bucket=new ItemStack(fluid.getFluid().getBucket());
        if(!bucket.isEmpty()&&tryUpdateFuel(bucket,false,true)&&tryUpdateFuel(bucket,false,false))
            tank.drain(1000,IFluidHandler.FluidAction.EXECUTE);
    }
    @Override public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip,boolean sneaking){
        dev.ccd.CCDTooltip.fluidContents(tooltip,tank.isEmpty()?java.util.List.of():java.util.List.of(tank.getFluid()),tank.getCapacity());
        dev.ccd.CCDTooltip.heading(tooltip,"ccd.tank_burner.heating");
        dev.ccd.CCDTooltip.line(tooltip,"ccd.tank_burner.heat",net.minecraft.network.chat.Component.translatable("ccd.tank_burner.heat."+getHeatLevelFromBlock().getSerializedName()));
        dev.ccd.CCDTooltip.line(tooltip,"ccd.tank_burner.remaining",dev.ccd.CCDTooltip.number((remainingBurnTime+19)/20));
        return true;
    }
}
