package dev.ccd.content;

import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Finite buffer supplied externally; rotational power only pumps stored fluid outward. */
public final class BufferPumpBlockEntity extends PumpBlockEntity {
    public final net.neoforged.neoforge.fluids.capability.templates.FluidTank buffer=new net.neoforged.neoforge.fluids.capability.templates.FluidTank(4000){
        @Override protected void onContentsChanged(){setChanged();if(level!=null&&!level.isClientSide())sendData();}
    };
    public BufferPumpBlockEntity(BlockPos pos,BlockState state){super(CopperDynamics.BUFFER_PUMP_BE.get(),pos,state);}
    @Override public boolean isSideAccessible(Direction side){return false;}
    @Override public void updatePressureChange(){} // No transparent bypass around the finite buffer.
    @Override public void tick(){
        super.tick();if(level==null||level.isClientSide()||getSpeed()==0||isOverStressed())return;
        Direction output=getBlockState().getValue(BufferPumpBlock.FACING);if(getSpeed()<0)output=output.getOpposite();
        int rate=Math.max(1,(int)(Math.abs(getSpeed())/2));
        var target=level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,worldPosition.relative(output),output.getOpposite());
        if(target!=null&&!buffer.isEmpty()){
            var offer=buffer.drain(rate,IFluidHandler.FluidAction.SIMULATE);
            int accepted=target.fill(offer,IFluidHandler.FluidAction.SIMULATE);
            if(accepted>0){var drained=buffer.drain(accepted,IFluidHandler.FluidAction.EXECUTE);int filled=target.fill(drained,IFluidHandler.FluidAction.EXECUTE);if(filled<drained.getAmount())buffer.fill(drained.copyWithAmount(drained.getAmount()-filled),IFluidHandler.FluidAction.EXECUTE);}
        }
    }
    @Override protected void write(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries,boolean packet){super.write(tag,registries,packet);tag.put("Buffer",buffer.writeToNBT(registries,new net.minecraft.nbt.CompoundTag()));}
    @Override protected void read(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries,boolean packet){super.read(tag,registries,packet);buffer.readFromNBT(registries,tag.getCompound("Buffer"));}
    @Override public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip,boolean sneaking){super.addToGoggleTooltip(tooltip,sneaking);if(!buffer.isEmpty())tooltip.add(buffer.getFluid().getHoverName());tooltip.add(net.minecraft.network.chat.Component.literal(buffer.getFluidAmount()+" / 4000 mB"));return true;}
}
