package dev.ccd.atomizing;

import java.util.List;
import com.simibubi.create.content.fluids.FluidFX;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** One properly registered native fan type, selected only by an atomizer's own air current. */
public final class AtomizingFanType implements FanProcessingType {
    @Override public boolean isValidAt(Level level, BlockPos pos) { return false; }
    @Override public int getPriority() { return 0; }
    @Override public boolean canProcess(ItemStack stack, Level level) { return AtomizerProcessing.canProcess(stack); }
    @Override public List<ItemStack> process(ItemStack stack, Level level) { return AtomizerProcessing.process(stack); }
    @Override public void affectEntity(Entity entity, Level level) {} // Owner applies scheduled, paid entity pulses.
    @Override public void spawnProcessingParticles(Level level, Vec3 pos) {
        var owner = AtomizerProcessing.source();
        if (owner != null && !owner.activeFluid().isEmpty())
            level.addParticle(FluidFX.getFluidParticle(owner.activeFluid()), pos.x, pos.y, pos.z, 0, .02, 0);
        else level.addParticle(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 0, .01, 0);
    }
    @Override public void morphAirFlow(AirFlowParticleAccess particle, RandomSource random) {
        particle.setColor(0xb8d8dc);
        particle.setAlpha(.4f);
    }
}
