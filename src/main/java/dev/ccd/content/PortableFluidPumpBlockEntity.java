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
    @Override public boolean isPullingOnSide(boolean front) {
        return super.isPullingOnSide(front)^(pumpingSpeed()<0);
    }
    @Override public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        // Native pump skips reversals when the absolute RPM is unchanged.
        if(level!=null&&!level.isClientSide()&&previousSpeed!=getSpeed()
            &&Math.abs(previousSpeed)==Math.abs(getSpeed()))updatePressureChange();
    }
    @Override protected double strokePhaseStep() {
        // The old 256 RPM cadence is the new low-speed baseline. Compress the
        // upper range to keep complete strokes readable rather than aliasing.
        double cyclesPerSecond=Math.min(3,Math.max(256/(120*Math.PI),
            256/(120*Math.PI)*Math.sqrt(Math.abs(pumpingSpeed())/4.0)));
        return cyclesPerSecond*Math.PI*2/20;
    }
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
        dev.ccd.CCDTooltip.heading(tooltip,"ccd.goggles.pumping");
        dev.ccd.CCDTooltip.line(tooltip,"ccd.reciprocating.flow",dev.ccd.CCDTooltip.number(targetFlow()));
        dev.ccd.CCDTooltip.line(tooltip,"ccd.reciprocating.range",dev.ccd.CCDTooltip.number(targetRange()));
        return true;
    }
}
