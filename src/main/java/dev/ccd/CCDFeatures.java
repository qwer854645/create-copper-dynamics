package dev.ccd;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Independent world settings. Registrations never depend on these switches. */
public final class CCDFeatures {
    private CCDFeatures() {}
    public static final Map<String, ModConfigSpec.BooleanValue> CONTENT = new LinkedHashMap<>();
    public static final java.util.Set<String> DEFAULT_DISABLED = java.util.Set.of(
        "turning_booster_pump", "vertical_turning_booster_pump", "omni_pump", "buffer_pump",
        "portable_fluid_pump", "variable_flow_pump", "flow_meter", "base_fluids",
        "nuclear_fluids", "nether_industry_fluids", "more_catalysts_fluids");
    private static volatile Map<String, Boolean> remote = Map.of();
    public static void remote(Map<String, Boolean> settings) { remote = Map.copyOf(settings); }
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue REBIRTH_NAME;
    public static final Map<String,String> FLUID_CATEGORIES = Map.ofEntries(
        Map.entry("slime","base_fluids"), Map.entry("haunting","base_fluids"), Map.entry("smoking_fluid","base_fluids"),
        Map.entry("enriching_reagent","nuclear_fluids"),
        Map.entry("soul_stripping_reagent","nether_industry_fluids"),
        Map.entry("explosive_slurry","more_catalysts_fluids"), Map.entry("resonant_solution","more_catalysts_fluids"),
        Map.entry("sculk_suspension","more_catalysts_fluids"), Map.entry("purifying_solution","more_catalysts_fluids"),
        Map.entry("withering_fluid","more_catalysts_fluids"), Map.entry("conduit_energy_fluid","more_catalysts_fluids"),
        Map.entry("moss_suspension","more_catalysts_fluids"), Map.entry("tear_solution","more_catalysts_fluids"),
        Map.entry("corrupting_fluid","more_catalysts_fluids"));
    static {
        var builder = new ModConfigSpec.Builder();
        builder.comment("Advanced pump variants, the flow meter and grouped fluid categories are disabled by default.",
            "Super Lava, Powder Snow fluid support and Quicksand have separate switches enabled by default. Other devices remain enabled.",
            "Restart the game/server after editing. Multiplayer uses the server's content settings.",
            "Disabled content keeps its registry IDs and saved contents; acquisition and normal operation stop.",
            "CCD acquisition/conversion recipes require every referenced content setting to be enabled.",
            "Devices have independent switches; fluids and their buckets share switches grouped by processing origin. No master switch.").push("content");
        section(builder,"transport","Pumps, valves and direct fluid transfer.",new String[]{
            "booster_pump", "turning_booster_pump", "vertical_turning_booster_pump",
            "bidirectional_fluid_pump", "redstone_fluid_pump", "omni_pump", "redstone_fluid_valve",
            "three_way_valve", "buffer_pump", "portable_fluid_pump", "variable_flow_pump", "fluid_transmitter",
            "creative_delivery_fluid_tank", "fluid_access_port", "fluid_bridge", "flow_meter"});
        section(builder,"storage","Storage, handheld access, collection and water supply.",new String[]{"smart_fluid_tank",
            "horizontal_smart_fluid_tank", "faucet", "smart_faucet", "fluid_hatch", "smart_fluid_hatch",
            "copper_collection_grate", "smart_collection_grate", "copper_water_basin", "copper_water_supply"});
        section(builder,"logistics","Fluid cargo handling and logistics. Package size variants share their material's setting.",new String[]{
            "fluid_arm", "fluid_packager", "fluid_repackager", "fluid_gauge", "fluid_package", "composite_package"});
        section(builder,"tools","Handheld fluid containers and filter configuration.",new String[]{
            "fluid_filter", "fluid_attribute_filter", "copper_bucket", "brass_bucket"});
        section(builder,"processing","Burner and atomizer devices; fluid media are configured separately by origin.",new String[]{"tank_blaze_burner","fluid_atomizer"});
        section(builder,"fluids","Processing origin categories. Each switch controls all listed source/flowing fluids, blocks, buckets, CCD recipes and effects. Does not disable the upstream mod.",new String[]{
            "seething_reagent","powder_snow","quicksand","base_fluids","nuclear_fluids","nether_industry_fluids","more_catalysts_fluids"});
        builder.pop();
        builder.comment("Optional cosmetic extras. These never change gameplay or registry IDs.").push("easterEggs");
        REBIRTH_NAME=builder.comment("Use the alternate Chinese mod title in the creative tab when the client language is Chinese.",
            "Off by default. English text, item names, recipes and saved data are unchanged. Restart the game to apply.")
            .translation("ccd.configuration.rebirthName").gameRestart().define("rebirthName",false);
        builder.pop();
        SPEC = builder.build();
    }
    private static void section(ModConfigSpec.Builder builder,String name,String comment,String[] ids){
        builder.comment(comment).translation("ccd.configuration.contentSection."+name).push(name);
        for(String id:ids)CONTENT.put(id,builder.comment(description(id))
            .translation("ccd.configuration.content."+id).gameRestart().define(id,!DEFAULT_DISABLED.contains(id)));
        builder.pop();
    }
    private static String description(String id){
        return switch(id){
            case "base_fluids" -> "CCD/base Create media: Slime Fluid, Soul Fluid and Smoking Fluid, including their buckets and effects.";
            case "seething_reagent" -> "Super Lava and its bucket. Controls fuel, seething processing and exposure effects, including standalone fuel use without Dungeons & Desires.";
            case "nuclear_fluids" -> "Create Nuclear medium: Enriching Reagent, its bucket and radiation exposure. Processing still requires Create Nuclear.";
            case "nether_industry_fluids" -> "Create Nether Industry medium: Soul Stripping Reagent and its bucket/effects. Processing still requires that addon.";
            case "more_catalysts_fluids" -> "Create: More Catalysts media: Explosive Slurry, Resonant Solution, Sculk Suspension, Purifying Solution, Withering Fluid, Conduit Energy Fluid, Moss Suspension, Tear Solution and Corrupting Fluid. Corrupting processing also requires AnvilCraft or L2 Hostility.";
            case "fluid_package" -> "Enable copper fluid packages, including all sizes. Required for fluid packaging and brass-package separation.";
            case "composite_package" -> "Enable brass mixed-cargo packages, including all sizes. Required for mixed-order repacking.";
            case "fluid_arm" -> "Enable the fluid arm and its internal carried cargo. Disabled arms retain their cargo for recovery.";
            case "fluid_gauge" -> "Enable fluid stock monitoring and ordering, including internal fluid stock tokens.";
            case "powder_snow" -> "Enable CCD fluid transport and processing support for powder snow. Does not disable the vanilla bucket or block.";
            case "copper_water_basin" -> "Enable the infinite water basin and manual/piped access. Does not control the Water Supply Box.";
            case "copper_water_supply" -> "Enable automatic adjacent water supply and the box's water interfaces. Supply rate is set in ccd-server.toml.";
            case "fluid_atomizer" -> "Enable fluid-powered airflow processing and entity effects. Fluid switches are configured below, individually or by origin.";
            case "booster_pump" -> "Enable the extended-pressure pump. Pressure, range and stress multipliers are configured in ccd-server.toml.";
            case "redstone_fluid_pump" -> "Enable the pump whose flow direction is reversed by a redstone signal.";
            case "bidirectional_fluid_pump" -> "Enable the pump whose flow follows its shaft rotation direction.";
            case "variable_flow_pump" -> "Enable signal-controlled pumping. Flow/range endpoints are configured in ccd-server.toml.";
            case "portable_fluid_pump" -> "Enable manually or mechanically driven pumping. Flow and range are configured in ccd-server.toml.";
            case "buffer_pump" -> "Enable the 2000 mB buffer and powered outlet; this pump does not actively extract from adjacent storage.";
            case "tank_blaze_burner" -> "Enable the Blaze Burner with a 4000 mB liquid-fuel reservoir and optional Liquid Fuel support.";
            case "fluid_filter" -> "Enable fluid sample filters. Disabled filters reject fluid instead of silently allowing everything.";
            case "fluid_attribute_filter" -> "Enable fluid attribute filters. Disabled filters reject fluid instead of silently allowing everything.";
            default -> "Enable " + id.replace('_',' ') + ".";
        };
    }
    public static boolean rebirthName(){
        try{return REBIRTH_NAME.get();}catch(IllegalStateException beforeConfigLoad){return false;}
    }
    public static String feature(String path) {
        if (path.startsWith("flowing_")) path = path.substring(8);
        if (path.endsWith("_bucket") && !path.equals("copper_bucket") && !path.equals("brass_bucket"))
            path = path.substring(0, path.length() - 7);
        if (path.startsWith("copper_package_")) return "fluid_package";
        if (path.startsWith("brass_package_")) return "composite_package";
        if (path.equals("fluid_arm_cargo")) return "fluid_arm";
        if (path.equals("fluid_stock")) return "fluid_gauge";
        return FLUID_CATEGORIES.getOrDefault(path,path);
    }
    public static boolean enabled(String path) {
        String key=feature(path);
        Boolean synced=remote.get(key);
        if(synced!=null)return synced;
        var value = CONTENT.get(key);
        if(value == null)return true;
        try { return value.get(); }
        catch (IllegalStateException unavailableBeforeWorldLoad) { return true; }
    }
    public static boolean enabled(ResourceLocation id) { return !id.getNamespace().equals(CopperDynamics.ID) || enabled(id.getPath()); }
    public static boolean enabled(net.minecraft.world.item.Item item) { return enabled(BuiltInRegistries.ITEM.getKey(item)); }
    public static boolean enabled(net.minecraft.world.level.block.state.BlockState state) { return enabled(BuiltInRegistries.BLOCK.getKey(state.getBlock())); }
    public static boolean enabled(net.minecraft.world.level.material.Fluid fluid) {
        if (fluid == CCDFluids.POWDER_SNOW.get() || fluid == CCDFluids.FLOWING_POWDER_SNOW.get()) return enabled("powder_snow");
        return enabled(BuiltInRegistries.FLUID.getKey(fluid));
    }
}
