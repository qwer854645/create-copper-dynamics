package dev.ccd.content;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Multi-port native pipe pump. No owned reservoir and no fluid capability. */
public final class OmniPumpBlockEntity extends KineticBlockEntity implements com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation {
    private boolean pressureDirty=true;
    public OmniPumpBlockEntity(BlockPos pos,BlockState state){super(CopperDynamics.OMNI_PUMP_BE.get(),pos,state);}
    public boolean powered(){return getSpeed()!=0&&!isOverStressed();}
    public boolean hasPorts(int mode){for(var side:Direction.values())if(OmniPumpBlock.mode(getBlockState(),side)==mode)return true;return false;}
    @Override public void addBehaviours(java.util.List<com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour> behaviours){
        super.addBehaviours(behaviours);
        behaviours.add(new FluidTransportBehaviour(this){
            public boolean canHaveFlowToward(BlockState state,Direction side){int mode=OmniPumpBlock.mode(state,side);return mode==OmniPumpBlock.INPUT||mode==OmniPumpBlock.OUTPUT;}
            @Override public boolean canPullFluidFrom(net.neoforged.neoforge.fluids.FluidStack fluid,BlockState state,Direction side){return powered()&&hasPorts(OmniPumpBlock.OUTPUT)&&OmniPumpBlock.mode(state,side)==OmniPumpBlock.INPUT;}
        });
    }
    public void invalidatePressure(){pressureDirty=true;}
    @Override public void onSpeedChanged(float previous){super.onSpeedChanged(previous);invalidatePressure();}
    @Override public void tick(){
        super.tick();if(level==null||level.isClientSide())return;
        if(!pressureDirty)return;
        pressureDirty=false;
        var pipe=getBehaviour(FluidTransportBehaviour.TYPE);
        for(var side:Direction.values())dev.ccd.logistics.NativeEndpointPump.changed(level,worldPosition.relative(side));
        pipe.wipePressure();
        if(!powered()||!hasPorts(OmniPumpBlock.INPUT)||!hasPorts(OmniPumpBlock.OUTPUT))return;
        var adapter=new dev.ccd.logistics.NativeEndpointPump(level,worldPosition,getBlockState());
        int inputs=0;for(var side:Direction.values())if(OmniPumpBlock.mode(getBlockState(),side)==OmniPumpBlock.INPUT)inputs++;
        for(var side:Direction.values()){
            int mode=OmniPumpBlock.mode(getBlockState(),side);
            if(mode!=OmniPumpBlock.INPUT&&mode!=OmniPumpBlock.OUTPUT)continue;
            float pressure=Math.abs(getSpeed())/inputs;
            adapter.distribute(side,mode==OmniPumpBlock.INPUT,pressure);
            pipe.addPressure(side,mode==OmniPumpBlock.INPUT,pressure);
        }
    }
    @Override public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip,boolean sneaking){
        super.addToGoggleTooltip(tooltip,sneaking);dev.ccd.CCDTooltip.heading(tooltip,"ccd.goggles.connections");
        for(var side:Direction.values())if(OmniPumpBlock.mode(getBlockState(),side)!=OmniPumpBlock.CLOSED)
            dev.ccd.CCDTooltip.line(tooltip,"ccd.omni_pump.face",net.minecraft.network.chat.Component.translatable("ccd.direction."+side.getName()),net.minecraft.network.chat.Component.translatable("ccd.omni_pump.mode."+OmniPumpBlock.mode(getBlockState(),side)));
        return true;
    }
}
