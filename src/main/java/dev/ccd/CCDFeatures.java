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
    private static volatile Map<String, Boolean> remote = Map.of();
    public static void remote(Map<String, Boolean> settings) { remote = Map.copyOf(settings); }
    public static final ModConfigSpec SPEC;
    static {
        var builder = new ModConfigSpec.Builder();
        for (String id : new String[]{
            "tank_blaze_burner", "booster_pump", "turning_booster_pump", "vertical_turning_booster_pump",
            "bidirectional_fluid_pump", "redstone_fluid_pump", "omni_pump", "redstone_fluid_valve",
            "three_way_valve", "buffer_pump", "portable_fluid_pump", "variable_flow_pump", "fluid_transmitter",
            "creative_delivery_fluid_tank", "fluid_access_port", "fluid_bridge", "smart_fluid_tank",
            "horizontal_smart_fluid_tank", "faucet", "smart_faucet", "fluid_hatch", "smart_fluid_hatch",
            "copper_collection_grate", "smart_collection_grate", "fluid_atomizer", "copper_water_basin",
            "copper_water_supply", "flow_meter", "fluid_arm", "fluid_packager", "fluid_repackager", "fluid_gauge",
            "fluid_package", "composite_package", "fluid_filter", "fluid_attribute_filter", "copper_bucket", "brass_bucket",
            "quicksand", "slime", "haunting", "powder_snow", "seething_reagent", "enriching_reagent",
            "soul_stripping_reagent", "explosive_slurry", "resonant_solution", "sculk_suspension", "purifying_solution",
            "withering_fluid", "conduit_energy_fluid", "moss_suspension", "tear_solution", "corrupting_fluid", "smoking_fluid"
        }) CONTENT.put(id, builder.comment("Enable " + id + ". Existing saved content is retained. Restart the game/server after changing.")
            .translation("ccd.configuration.content." + id).gameRestart().define(id, true));
        SPEC = builder.build();
    }
    public static String feature(String path) {
        if (path.startsWith("flowing_")) path = path.substring(8);
        if (path.endsWith("_bucket") && !path.equals("copper_bucket") && !path.equals("brass_bucket"))
            path = path.substring(0, path.length() - 7);
        if (path.startsWith("copper_package_")) return "fluid_package";
        if (path.startsWith("brass_package_")) return "composite_package";
        if (path.equals("fluid_arm_cargo")) return "fluid_arm";
        if (path.equals("fluid_stock")) return "fluid_gauge";
        return path;
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
