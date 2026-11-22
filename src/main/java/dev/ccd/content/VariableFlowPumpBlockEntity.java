package dev.ccd.content;

import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import dev.ccd.CCDConfig;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class VariableFlowPumpBlockEntity extends PumpBlockEntity {
    public float previousStroke,stroke;
    public float previousSignalDial,signalDial;
    private boolean signalDialInitialized;
    private double phase;
    private double previousPhase;
    public double visualPhase(float partialTicks) { return previousPhase+(phase-previousPhase)*partialTicks; }
    public VariableFlowPumpBlockEntity(BlockPos pos,BlockState state) { this(CopperDynamics.VARIABLE_FLOW_PUMP_BE.get(),pos,state); }
    protected VariableFlowPumpBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,BlockPos pos,BlockState state) { super(type,pos,state); }
    public float pumpingSpeed() {
        return getSpeed();
    }
    protected double strokePhaseStep() { return Math.abs(pumpingSpeed())/1200.0; }
    public int targetFlow() { return CCDConfig.variableFlow(getBlockState().getValue(VariableFlowPumpBlock.SIGNAL)); }
    public int targetRange() { return CCDConfig.variableRange(getBlockState().getValue(VariableFlowPumpBlock.SIGNAL)); }
    public float flowPressure() { return pumpingSpeed()==0||isOverStressed()||targetRange()==0?0:targetFlow()*2f; }
    @Override protected void distributePressureTo(net.minecraft.core.Direction side) {
        if(flowPressure()>0)super.distributePressureTo(side);
    }
    @Override public void initialize() { super.initialize();if(level!=null&&!level.isClientSide())updatePressureChange(); }
    @Override public void tick() {
        super.tick();
        if(level!=null&&(level.isClientSide()||isVirtual())){
            float target=getBlockState().getValue(VariableFlowPumpBlock.SIGNAL)/15f;
            if(!signalDialInitialized){signalDial=previousSignalDial=target;signalDialInitialized=true;}
            previousSignalDial=signalDial;
            signalDial+=(target-signalDial)*.25f;
        }
        if(level!=null&&(level.isClientSide()||isVirtual()||this instanceof PortableFluidPumpBlockEntity)) {
            previousStroke=stroke;
            previousPhase=phase;
            if(pumpingSpeed()!=0&&!isOverStressed()&&targetFlow()>0&&targetRange()>0) {
                phase+=strokePhaseStep();
                stroke=(float)((1-Math.cos(phase))*.5);
            } else stroke+=(0-stroke)*.25f;
        }
    }
    @Override public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip,boolean sneaking) {
        super.addToGoggleTooltip(tooltip,sneaking);
        dev.ccd.CCDTooltip.heading(tooltip,"ccd.goggles.pumping");
        dev.ccd.CCDTooltip.line(tooltip,"ccd.variable_flow.flow",dev.ccd.CCDTooltip.number(targetFlow()));
        dev.ccd.CCDTooltip.line(tooltip,"ccd.variable_flow.signal",dev.ccd.CCDTooltip.number(getBlockState().getValue(VariableFlowPumpBlock.SIGNAL)));
        dev.ccd.CCDTooltip.line(tooltip,"ccd.variable_flow.range",dev.ccd.CCDTooltip.number(targetRange()));
        return true;
    }
}
