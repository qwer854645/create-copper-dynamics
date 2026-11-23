package dev.ccd.atomizing;

import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.equipment.sandPaper.SandPaperPolishingRecipe;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import dev.ccd.CCDFluids;
import dev.ccd.CopperDynamics;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Reads the live sandpaper recipe type directly; no cloned processing recipes. */
public final class SandblastingType implements FanProcessingType {
    public static FanProcessingType processingType() {
        var type=dev.ccd.compat.FanCompat.sanding();
        if(type!=null) return type;
        return CopperDynamics.SANDBLASTING.get();
    }
    @Override public boolean isValidAt(Level level,BlockPos pos) {
        if(net.neoforged.fml.ModList.get().isLoaded("create_connected")
            && net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock())
                .equals(dev.ccd.compat.ConnectedQuicksandInteraction.SANDING)) return true;
        var state=level.getFluidState(pos);
        return dev.ccd.CCDFeatures.enabled("quicksand") && !state.isEmpty() && state.getType().getFluidType()==CCDFluids.QUICKSAND_TYPE.get();
    }
    @Override public int getPriority() { return 450; }
    @Override public boolean canProcess(ItemStack stack,Level level) {
        return !SandPaperPolishingRecipe.getMatchingRecipes(level,stack).isEmpty();
    }
    @Override public List<ItemStack> process(ItemStack stack,Level level) {
        var recipes=SandPaperPolishingRecipe.getMatchingRecipes(level,stack);
        return recipes.isEmpty()?null:RecipeApplier.applyRecipeOn(level,stack,recipes.getFirst().value(),false);
    }
    @Override public void spawnProcessingParticles(Level level,Vec3 pos) { level.addParticle(ParticleTypes.ASH,pos.x,pos.y,pos.z,0,.02,0); }
    @Override public void morphAirFlow(AirFlowParticleAccess particle,RandomSource random) { particle.setColor(0xd8c28b); particle.setAlpha(.5f); }
    @Override public void affectEntity(Entity entity,Level level) {}
}
