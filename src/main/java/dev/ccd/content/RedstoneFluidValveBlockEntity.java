package dev.ccd.content;

import java.util.List;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlockEntity;
import dev.ccd.CopperDynamics;
import dev.ccd.CCDTooltip;
import dev.ccd.mixin.FluidValvePointerAccessor;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/** Drive the existing pointer; native tick owns ENABLED changes and the pipe behaviour remains inherited. */
public final class RedstoneFluidValveBlockEntity extends FluidValveBlockEntity {
    public RedstoneFluidValveBlockEntity(BlockPos pos, BlockState state) {
        super(CopperDynamics.REDSTONE_FLUID_VALVE_BE.get(), pos, state);
    }
    @Override public void initialize() {
        super.initialize();
        if (level != null) RedstoneFluidValveBlock.updateSignal(level, worldPosition);
    }
    @Override public void tick() {
        if (!dev.ccd.CCDFeatures.enabled(getBlockState())) return;
        drivePointer();
        super.tick();
    }
    @Override public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        // The inherited kinetic callback must not replace the redstone actuator target.
        drivePointer();
    }
    private void drivePointer() {
        boolean powered = getBlockState().getValue(RedstoneFluidValveBlock.POWERED);
        if(net.neoforged.fml.ModList.get().isLoaded("pipesnphysics"))
            ((de.devin.pipesnphysics.engine.valve.ValveThrottle)(Object)this).pipesnphysics$adjustThrottle(powered?-90:90);
        ((FluidValvePointerAccessor) (Object) this).ccd$getPointer().chase(powered ? 0 : 1, 1, Chaser.LINEAR);
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        CCDTooltip.heading(tooltip, "ccd.gui.goggles.valve");
        boolean open = getBlockState().getValue(RedstoneFluidValveBlock.ENABLED);
        CCDTooltip.line(tooltip, open ? ChatFormatting.AQUA : ChatFormatting.GRAY, open ? "ccd.valve.open" : "ccd.valve.closed");
        return true;
    }
}
