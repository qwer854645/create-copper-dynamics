package dev.ccd.compat;

import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import dev.ccd.CCDFluids;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;

/** Explicit registry contracts. No optional mod classes or guessed path scanning. */
public final class FanCompat {
    public record Rule(String mod,String type,String fluidTag,String catalyst) {}
    public static final List<Rule> RULES=List.of(
        new Rule("create_dragons_plus","ending","fan_processing_catalysts/ending","fan_ending_catalyst_dragons_breath"),
        new Rule("dndesires","dragon_breathing","fan_processing_catalysts/dragon_breathing","fan_ending_catalyst_dragons_breath"),
        new Rule("dndesires","seething","fan_processing_catalysts/seething","fan_seething_catalyst"),
        new Rule("create_dragons_plus","freezing","fan_processing_catalysts/freezing","fan_freezing_catalyst"),
        new Rule("dndesires","freezing","fan_processing_catalysts/freezing","fan_freezing_catalyst"),
        new Rule("garnished","freezing","fan_processing_catalysts/freezing","fan_freezing_catalyst"),
        new Rule("create_shimmer","glooming","fan_glooming_catalysts","fan_glooming_catalyst"),
        new Rule("create_shimmer","transmutation","fan_transmutation_catalysts","fan_transmutation_catalyst"),
        // Block-only upstream media: explicitly opt in fluids through CCD's tags.
        new Rule("createnuclear","enriched","@ccd:fan_media/enriched","fan_enriched_catalyst"),
        new Rule("createnetherindustry","soul_stripping","@ccd:fan_media/soul_stripping","fan_soul_stripping_catalyst"),
        new Rule("create_more_catalysts","honey_coating","fan_catalysts/honey_coating","fan_honey_coating_catalyst"),
        new Rule("create_more_catalysts","chocolate_coating","fan_catalysts/chocolate_coating","fan_chocolate_coating_catalyst"),
        new Rule("create_more_catalysts","exploding","fan_catalysts/exploding","fan_exploding_catalyst"),
        new Rule("create_more_catalysts","resonance","fan_catalysts/resonance","fan_resonance_catalyst"),
        new Rule("create_more_catalysts","sculking","fan_catalysts/sculking","fan_sculking_catalyst"),
        new Rule("create_more_catalysts","purifying","fan_catalysts/purifying","fan_purifying_catalyst"),
        new Rule("create_more_catalysts","withering","fan_catalysts/withering","fan_withering_catalyst"),
        new Rule("create_more_catalysts","tidal","fan_catalysts/tidal","fan_tidal_catalyst"),
        new Rule("create_more_catalysts","mossify","fan_catalysts/mossify","fan_mossify_catalyst"),
        new Rule("create_more_catalysts","tear","fan_catalysts/tear","fan_tear_catalyst"),
        new Rule("create_more_catalysts","corrupted","fan_catalysts/corrupted","fan_corrupted_catalyst")
    );
    private FanCompat() {}
    public static FanProcessingType find(String mod,String path) {
        if(!ModList.get().isLoaded(mod)) return null;
        var id=ResourceLocation.fromNamespaceAndPath(mod,path);
        return CreateBuiltInRegistries.FAN_PROCESSING_TYPE.containsKey(id)?CreateBuiltInRegistries.FAN_PROCESSING_TYPE.get(id):null;
    }
    public static FanProcessingType sanding() {
        var result=find("create_dragons_plus","sanding");
        return result!=null?result:find("dndesires","sanding");
    }
    public static FanProcessingType freezing() {
        var result=find("create_dragons_plus","freezing");
        if(result!=null) return result;
        result=find("dndesires","freezing");
        return result!=null?result:find("garnished","freezing");
    }
    public static boolean tagged(FluidStack fluid,String mod,String tag) {
        if(tag.startsWith("@")) return fluid.getFluid().builtInRegistryHolder().is(TagKey.create(Registries.FLUID,ResourceLocation.parse(tag.substring(1))));
        return fluid.getFluid().builtInRegistryHolder().is(TagKey.create(Registries.FLUID,ResourceLocation.fromNamespaceAndPath(mod,tag)));
    }
    public static FanProcessingType select(FluidStack fluid) {
        if(fluid.getFluid().getFluidType()==CCDFluids.POWDER_SNOW_TYPE.get()) return freezing();
        for(var rule:RULES) if(tagged(fluid,rule.mod(),rule.fluidTag())) {
            var type=find(rule.mod(),rule.type()); if(type!=null) return type;
        }
        // Vanilla dye variants only; excluded dye expansions are not inferred.
        for(var color:DyeColor.values()) if(tagged(fluid,"c","dyes/"+color.getName()) || tagged(fluid,"garnished","fan_processing_catalysts/dye/"+color.getName())) {
            var type=coloring(color);
            if(type!=null) return type;
        }
        if(tagged(fluid,"c","dragon_breath")) return find("create_dragons_plus","ending");
        var fluidId=net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid.getFluid());
        if(fluidId.getNamespace().equals("create")) {
            if(fluidId.getPath().equals("honey") || fluidId.getPath().equals("flowing_honey")) return find("create_more_catalysts","honey_coating");
            if(fluidId.getPath().equals("chocolate") || fluidId.getPath().equals("flowing_chocolate")) return find("create_more_catalysts","chocolate_coating");
        }
        return null;
    }
    private static FanProcessingType coloring(DyeColor color) {
        var type=find("create_dragons_plus","coloring_"+color.getName());
        return type!=null?type:find("garnished",color.getName()+"_dye_blowing");
    }
    public static String catalyst(FluidStack fluid) {
        if(fluid.getFluid().getFluidType()==CCDFluids.QUICKSAND_TYPE.get()) return "fan_sanding_catalyst";
        if(fluid.getFluid().getFluidType()==CCDFluids.POWDER_SNOW_TYPE.get() && freezing()!=null) return "fan_freezing_catalyst";
        for(var rule:RULES) if(tagged(fluid,rule.mod(),rule.fluidTag()) && find(rule.mod(),rule.type())!=null) return rule.catalyst();
        for(var color:DyeColor.values()) if((tagged(fluid,"c","dyes/"+color.getName()) || tagged(fluid,"garnished","fan_processing_catalysts/dye/"+color.getName())) && coloring(color)!=null)
            return color.getName()+"_fan_dyeing_catalyst";
        if(tagged(fluid,"c","dragon_breath") && find("create_dragons_plus","ending")!=null) return "fan_ending_catalyst_dragons_breath";
        var selected=select(fluid);
        if(selected!=null && selected==find("create_more_catalysts","honey_coating")) return "fan_honey_coating_catalyst";
        if(selected!=null && selected==find("create_more_catalysts","chocolate_coating")) return "fan_chocolate_coating_catalyst";
        return null;
    }
}
