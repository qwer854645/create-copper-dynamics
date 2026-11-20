package dev.ccd.content;

import dev.ccd.CopperDynamics;
import dev.ccd.CCDConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;

/** Manual pressure is internal: it never generates rotation into the shaft network. */
public final class PortableFluidPumpBlockEntity extends ReciprocatingFluidPumpBlockEntity {
    private int manualTicks;
    private boolean backwards;
    public PortableFluidPumpBlockEntity(BlockPos pos,BlockState state) { super(CopperDynamics.PORTABLE_PUMP_BE.get(),pos,state); }
    @Override public int targetFlow() { return CCDConfig.portableFlow(); }
    @Override public int targetRange() { return CCDConfig.PORTABLE_RANGE.get(); }
    @Override public float pumpingSpeed() { return getSpeed()!=0?getSpeed():manualTicks>0?(backwards?-32:32):0; }
    @Override public boolean isPullingOnSide(boolean front) { return super.isPullingOnSide(front)^(getSpeed()==0&&manualTicks>0&&backwards); }
    public void turn(boolean reverse) {
        boolean changed=manualTicks==0||backwards!=reverse;
        manualTicks=10;backwards=reverse;
        if(changed)updatePressureChange();
        sendData();
    }
    @Override public void tick() {
        super.tick();
        if(manualTicks>0&&--manualTicks==0&&level!=null&&!level.isClientSide()) { updatePressureChange();sendData(); }
    }
    @Override protected void write(CompoundTag tag,HolderLookup.Provider registries,boolean packet) {
        super.write(tag,registries,packet);
        if(packet) { tag.putInt("ManualTicks",manualTicks);tag.putBoolean("ManualBackwards",backwards); }
    }
    @Override protected void read(CompoundTag tag,HolderLookup.Provider registries,boolean packet) {
        super.read(tag,registries,packet);
        if(packet) { manualTicks=tag.getInt("ManualTicks");backwards=tag.getBoolean("ManualBackwards"); }
    }
    @Override public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip,boolean sneaking) {
        tooltip.add(net.minecraft.network.chat.Component.translatable("ccd.reciprocating.flow",flowPressure()>0?targetFlow():0));
        tooltip.add(net.minecraft.network.chat.Component.translatable("ccd.reciprocating.range",targetRange()));
        return true;
    }
}
