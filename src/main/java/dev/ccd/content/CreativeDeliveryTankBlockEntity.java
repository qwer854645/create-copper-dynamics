package dev.ccd.content;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Infinite creative source with bounded pipe traversal; no owned finite cargo. */
public final class CreativeDeliveryTankBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private FluidStack selected=FluidStack.EMPTY;
    public FluidStack displayFluid(){return selected.copy();}
    public static final int RANGE=256, RATE=1000000, VISIT_LIMIT=4096;
    public CreativeDeliveryTankBlockEntity(BlockPos pos,BlockState state){super(CopperDynamics.CREATIVE_DELIVERY_TANK_BE.get(),pos,state);}
    public com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour worldOutput;
    @Override public void addBehaviours(java.util.List<com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour> behaviours){
        behaviours.add(new dev.ccd.logistics.StoredSourceTransport(this,()->getBlockState().getValue(CreativeDeliveryTankBlock.FACING),()->handler,()->selected.isEmpty()?0f:RATE*2f));
        behaviours.add(worldOutput=new com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour<OutputMode>(OutputMode.class,
            net.minecraft.network.chat.Component.translatable("ccd.creative_delivery.mode"),this,
            new CreativeDeliverySlotTransform()));
    }
    public enum OutputMode implements com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions {
        CONTAINERS(com.simibubi.create.foundation.gui.AllIcons.I_FILL,"ccd.creative_delivery.containers_only"),
        WORLD(com.simibubi.create.foundation.gui.AllIcons.I_PLACE,"ccd.creative_delivery.allow_world");
        private final com.simibubi.create.foundation.gui.AllIcons icon;
        private final String key;
        OutputMode(com.simibubi.create.foundation.gui.AllIcons icon,String key){this.icon=icon;this.key=key;}
        @Override public com.simibubi.create.foundation.gui.AllIcons getIcon(){return icon;}
        @Override public String getTranslationKey(){return key;}
    }
    private static net.minecraft.network.chat.MutableComponent modeName(int value){return net.minecraft.network.chat.Component.translatable(value==0?"ccd.creative_delivery.containers_only":"ccd.creative_delivery.allow_world");}
    public void select(FluidStack fluid){invalidatePressure();selected=fluid.isEmpty()?FluidStack.EMPTY:fluid.copyWithAmount(1000);notifyUpdate();if(level!=null)level.invalidateCapabilities(worldPosition);}
    public final IFluidHandler handler=new IFluidHandler(){
        public int getTanks(){return 1;}
        public FluidStack getFluidInTank(int tank){return selected.isEmpty()?FluidStack.EMPTY:selected.copyWithAmount(Integer.MAX_VALUE);}
        public int getTankCapacity(int tank){return Integer.MAX_VALUE;}
        public boolean isFluidValid(int tank,FluidStack fluid){return false;}
        public int fill(FluidStack fluid,FluidAction action){return 0;}
        public FluidStack drain(int amount,FluidAction action){return amount<=0||selected.isEmpty()?FluidStack.EMPTY:selected.copyWithAmount(amount);}
        public FluidStack drain(FluidStack fluid,FluidAction action){return FluidStack.isSameFluidSameComponents(fluid,selected)?drain(fluid.getAmount(),action):FluidStack.EMPTY;}
    };
    private boolean pressureDirty=true;
    public void invalidatePressure(){pressureDirty=true;}
    @Override public void tick(){
        if (!dev.ccd.CCDFeatures.enabled(getBlockState())) return;
        super.tick();if(level==null||level.isClientSide())return;
        if(!pressureDirty)return;
        pressureDirty=false;
        var side=getBlockState().getValue(CreativeDeliveryTankBlock.FACING);var next=worldPosition.relative(side);
        dev.ccd.logistics.NativeEndpointPump.changed(level,next);
        if(!selected.isEmpty())new dev.ccd.logistics.NativeEndpointPump(level,worldPosition,getBlockState()).distribute(side,false,RATE*2f);
    }
    @Override protected void write(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries,boolean packet){super.write(tag,registries,packet);tag.put("SelectedFluid",selected.saveOptional(registries));}
    @Override protected void read(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries,boolean packet){super.read(tag,registries,packet);selected=FluidStack.parseOptional(registries,tag.getCompound("SelectedFluid"));}
    @Override public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip,boolean sneaking){
        dev.ccd.CCDTooltip.heading(tooltip,"create.gui.goggles.fluid_container");
        if(selected.isEmpty())dev.ccd.CCDTooltip.line(tooltip,"ccd.creative_delivery.empty");
        else{
            com.simibubi.create.foundation.utility.CreateLang.fluidName(selected).style(net.minecraft.ChatFormatting.GRAY).forGoggles(tooltip,1);
            dev.ccd.CCDTooltip.line(tooltip,net.minecraft.ChatFormatting.GOLD,"ccd.creative_delivery.active");
        }
        dev.ccd.CCDTooltip.line(tooltip,"ccd.goggles.output_mode",modeName(worldOutput.getValue()));return true;
    }
}
