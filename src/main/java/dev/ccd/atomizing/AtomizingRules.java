package dev.ccd.atomizing;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import com.simibubi.create.AllFluids;
import com.simibubi.create.AllTags.AllFluidTags;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import dev.ccd.CCDConfig;
import dev.ccd.CopperDynamics;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

public final class AtomizingRules {
    private AtomizingRules() {}
    private static final AtomicLong REVISION = new AtomicLong();
    private record Cached(long revision, List<RecipeHolder<AtomizingRecipe>> recipes) {}
    private static final Map<RecipeManager, Cached> CACHES = new WeakHashMap<>();
    public record Selection(String id, AtomizingRecipe recipe, FanProcessingType type, int amount, int interval, boolean potion) {
        public boolean hasAction() { return type != null || potion || recipe != null
            && (!recipe.effects().isEmpty() || recipe.damage() > 0 || !recipe.entityScript().isEmpty()); }
    }
    public static long revision() { return REVISION.get(); }
    public static void invalidate() { REVISION.incrementAndGet(); }
    public static synchronized Selection select(Level level, FluidStack fluid) {
        if (fluid.isEmpty()) return null;
        var manager = level.getRecipeManager();
        Cached cached = CACHES.get(manager);
        if (cached == null || cached.revision != revision()) {
            var recipes = manager.getAllRecipesFor(CopperDynamics.ATOMIZING_TYPE.get()).stream()
                .sorted(Comparator.<RecipeHolder<AtomizingRecipe>>comparingInt(h -> h.value().priority()).reversed()
                    .thenComparing(h -> h.id().toString())).toList();
            CACHES.put(manager, cached = new Cached(revision(), recipes));
        }
        for (var holder : cached.recipes) if (holder.value().fluid().test(fluid)) {
            var recipe = holder.value();
            if (!recipe.enabled()) return null;
            FanProcessingType type = recipe.items().isEmpty() ? parseType(recipe.processingType()) : CopperDynamics.ATOMIZING_FAN_TYPE.get();
            // The shared adapter is selected by item rules, never by referring back to itself.
            if (recipe.items().isEmpty() && type == CopperDynamics.ATOMIZING_FAN_TYPE.get()) type = null;
            return new Selection(holder.id().toString(), recipe, type, recipe.consumption().amount(), recipe.consumption().interval(), false);
        }
        if (AllFluids.POTION.is(fluid.getFluid()))
            return new Selection("ccd:potions", null, null, CCDConfig.ATOMIZER_POTION_COST.get(), 20, true);
        if(fluid.getFluid().getFluidType()==dev.ccd.CCDFluids.QUICKSAND_TYPE.get())
            return new Selection("ccd:quicksand",null,SandblastingType.processingType(),CCDConfig.ATOMIZER_NORMAL_COST.get(),20,false);
        // Native fluid catalyst tags and priority, without fabricating blocks or a fake level.
        var compatible=dev.ccd.compat.FanCompat.select(fluid);
        if(compatible!=null) return new Selection("ccd:compat/"+com.simibubi.create.api.registry.CreateBuiltInRegistries.FAN_PROCESSING_TYPE.getKey(compatible),
            null,compatible,CCDConfig.ATOMIZER_NORMAL_COST.get(),20,false);
        FanProcessingType type = null;
        var f = fluid.getFluid();
        if (AllFluidTags.FAN_PROCESSING_CATALYSTS_BLASTING.matches(f)) type = AllFanProcessingTypes.BLASTING;
        if (AllFluidTags.FAN_PROCESSING_CATALYSTS_SMOKING.matches(f) && (type == null || type.getPriority() < AllFanProcessingTypes.SMOKING.getPriority())) type = AllFanProcessingTypes.SMOKING;
        if (AllFluidTags.FAN_PROCESSING_CATALYSTS_HAUNTING.matches(f) && (type == null || type.getPriority() < AllFanProcessingTypes.HAUNTING.getPriority())) type = AllFanProcessingTypes.HAUNTING;
        if (AllFluidTags.FAN_PROCESSING_CATALYSTS_SPLASHING.matches(f) && (type == null || type.getPriority() < AllFanProcessingTypes.SPLASHING.getPriority())) type = AllFanProcessingTypes.SPLASHING;
        return type == null ? null : new Selection("ccd:native", null, type, CCDConfig.ATOMIZER_NORMAL_COST.get(), 20, false);
    }
    public static FanProcessingType parseType(String id) {
        return id.isEmpty() || net.minecraft.resources.ResourceLocation.tryParse(id) == null ? null : FanProcessingType.parse(id);
    }
}
