package dev.ccd.content;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

public final class FluidFilterRules {
    public static final int SLOTS = 18;
    public static List<CompoundTag> choices(FluidStack fluid) {
        var result = new ArrayList<CompoundTag>();
        if (fluid.isEmpty()) return result;
        var id = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
        result.add(rule("mod", id.getNamespace()));
        fluid.getFluid().builtInRegistryHolder().tags().sorted(java.util.Comparator.comparing(t -> t.location().toString()))
            .forEach(tag -> result.add(rule("tag", tag.location().toString())));
        result.add(rule("temperature", fluid.getFluidType().getTemperature(fluid) >= 1000 ? "hot" : "normal"));
        result.add(rule("density", fluid.getFluidType().getDensity(fluid) < 0 ? "gas" : "liquid"));
        result.add(rule("light", fluid.getFluidType().getLightLevel(fluid) > 0 ? "lit" : "dark"));
        return result;
    }
    public static CompoundTag rule(String kind, String value) {
        var tag = new CompoundTag(); tag.putString("kind", kind); tag.putString("value", value); return tag;
    }
    public static boolean matches(CompoundTag settings, boolean attributes, Level level, FluidStack fluid) {
        if(!dev.ccd.CCDFeatures.enabled(attributes?"fluid_attribute_filter":"fluid_filter"))return false;
        if (fluid.isEmpty()) return false;
        var entries = settings.getList("entries", Tag.TAG_COMPOUND);
        boolean all = settings.getBoolean("all"), result = all;
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.getCompound(i);
            boolean match;
            if (attributes) {
                match = attribute(entry, fluid);
                if (entry.getBoolean("invert")) match = !match;
            } else {
                FluidStack sample = FluidStack.parseOptional(level.registryAccess(), entry.getCompound("fluid"));
                match = settings.getBoolean("components") ? FluidStack.isSameFluidSameComponents(sample, fluid)
                    : !sample.isEmpty() && sample.getFluid().isSame(fluid.getFluid());
            }
            if (all) result &= match; else result |= match;
        }
        // Empty whitelist matches nothing, empty blacklist matches everything.
        if (entries.isEmpty()) result = false;
        return settings.getBoolean("blacklist") ? !result : result;
    }
    private static boolean attribute(CompoundTag rule, FluidStack fluid) {
        String value = rule.getString("value");
        return switch (rule.getString("kind")) {
            case "mod" -> BuiltInRegistries.FLUID.getKey(fluid.getFluid()).getNamespace().equals(value);
            case "tag" -> ResourceLocation.tryParse(value) != null && fluid.getFluid().is(TagKey.create(Registries.FLUID, ResourceLocation.parse(value)));
            case "temperature" -> (fluid.getFluidType().getTemperature(fluid) >= 1000 ? "hot" : "normal").equals(value);
            case "density" -> (fluid.getFluidType().getDensity(fluid) < 0 ? "gas" : "liquid").equals(value);
            case "light" -> (fluid.getFluidType().getLightLevel(fluid) > 0 ? "lit" : "dark").equals(value);
            default -> false;
        };
    }
}
