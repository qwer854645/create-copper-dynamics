package dev.ccd.content;

import dev.ccd.CopperDynamics;
import dev.ccd.CCDConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;

/** Pump with a short-lived hand-crank source on its existing shaft network. */
public final class PortableFluidPumpBlockEntity extends ReciprocatingFluidPumpBlockEntity {
    private int manualTicks;
    private boolean backwards;
    public PortableFluidPumpBlockEntity(BlockPos pos,BlockState state) { super(CopperDynamics.PORTABLE_PUMP_BE.get(),pos,state); }
    @Override public int targetFlow() { return CCDConfig.portableFlow(); }
    @Override public int targetRange() { return CCDConfig.PORTABLE_RANGE.get(); }
    @Override public float pumpingSpeed() { return getSpeed(); }
    @Override public float getGeneratedSpeed() { return manualTicks>0?(backwards?-32:32):0; }
    public void updateHandRotation() {
        if(level==null||level.isClientSide())return;
        float old=getTheoreticalSpeed(),generated=getGeneratedSpeed();
        // External drives keep control; do not fight an existing source by hand.
        if(hasSource()) {
            getOrCreateNetwork().updateCapacityFor(this,generated==0?0:calculateAddedStressCapacity());
            getOrCreateNetwork().updateStress();sendData();return;
        }
        if(old!=generated) {
            detachKinetics();setSpeed(generated);
            setNetwork(generated==0?null:Long.valueOf(worldPosition.asLong()));
            if(generated!=0)attachKinetics();
            onSpeedChanged(old);
        }
        if(hasNetwork()) {
            var network=getOrCreateNetwork();
            network.updateCapacityFor(this,generated==0?0:calculateAddedStressCapacity());
            network.updateStressFor(this,calculateStressApplied());network.updateStress();
        }
        sendData();
    }
    public void turn(boolean reverse) {
        boolean changed=manualTicks==0||backwards!=reverse;
        manualTicks=10;backwards=reverse;
        updateHandRotation();
        if(changed)updatePressureChange();
        sendData();
    }
    @Override public void tick() {
        super.tick();
        if(manualTicks>0&&--manualTicks==0&&level!=null&&!level.isClientSide()) { updateHandRotation();updatePressureChange();sendData(); }
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
