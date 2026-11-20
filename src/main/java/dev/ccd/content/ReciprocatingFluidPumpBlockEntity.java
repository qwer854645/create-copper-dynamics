package dev.ccd.content;

import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import dev.ccd.CCDConfig;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class ReciprocatingFluidPumpBlockEntity extends PumpBlockEntity {
    public float previousStroke,stroke;
    private double phase;
    public ReciprocatingFluidPumpBlockEntity(BlockPos pos,BlockState state) { super(CopperDynamics.RECIPROCATING_PUMP_BE.get(),pos,state); }
    public int targetFlow() { return CCDConfig.reciprocatingFlow(getBlockState().getValue(ReciprocatingFluidPumpBlock.SIGNAL)); }
    public float flowPressure() { return getSpeed()==0||isOverStressed()?0:targetFlow()*2f; }
    @Override protected void distributePressureTo(net.minecraft.core.Direction side) {
        if(flowPressure()>0)super.distributePressureTo(side);
    }
    @Override public void initialize() { super.initialize();if(level!=null&&!level.isClientSide())updatePressureChange(); }
    @Override public void tick() {
        super.tick();
        if(level!=null&&(level.isClientSide()||isVirtual())) {
            previousStroke=stroke;
            if(getSpeed()!=0&&!isOverStressed()&&targetFlow()>0) {
                phase=(phase+Math.abs(getSpeed())/1200.0)%(Math.PI*2);
                stroke=(float)((1-Math.cos(phase))*.5);
            } else stroke+=(0-stroke)*.25f;
        }
    }
    @Override public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip,boolean sneaking) {
        super.addToGoggleTooltip(tooltip,sneaking);
        tooltip.add(net.minecraft.network.chat.Component.translatable("ccd.reciprocating.flow",getSpeed()==0||isOverStressed()?0:targetFlow())
            .withStyle(net.minecraft.ChatFormatting.AQUA));
        tooltip.add(net.minecraft.network.chat.Component.translatable("ccd.reciprocating.signal",getBlockState().getValue(ReciprocatingFluidPumpBlock.SIGNAL))
            .withStyle(net.minecraft.ChatFormatting.GRAY));
        return true;
    }
}
