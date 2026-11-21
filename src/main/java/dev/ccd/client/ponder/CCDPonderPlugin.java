package dev.ccd.client.ponder;

import dev.ccd.CopperDynamics;
import dev.ccd.content.MetalPackages;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public final class CCDPonderPlugin implements PonderPlugin {
    @Override public String getModId() { return CopperDynamics.ID; }
    @Override public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        scene(helper,"packaging",CCDPonderScenes::packaging,"fluid_packager","fluid_repackager","fluid_package","composite_package");
        scene(helper,"hatches",CCDPonderScenes::hatches,"fluid_hatch","smart_fluid_hatch","fluid_arm");
        scene(helper,"access",CCDPonderScenes::access,"fluid_access_port","fluid_bridge");
        scene(helper,"tanks",CCDPonderScenes::tanks,"smart_fluid_tank","horizontal_smart_fluid_tank");
        scene(helper,"faucets",CCDPonderScenes::faucets,"faucet","smart_faucet");
        scene(helper,"atomizing",CCDPonderScenes::atomizing,"fluid_atomizer");
        scene(helper,"atomizing",CCDPonderScenes::gluing,"slime_bucket");
        scene(helper,"valves",CCDPonderScenes::valves,"three_way_valve");
        scene(helper,"collection",CCDPonderScenes::collection,"copper_collection_grate","smart_collection_grate","copper_water_basin","copper_water_supply");
        scene(helper,"transfer",CCDPonderScenes::transfer,"fluid_transmitter");
        scene(helper,"pumping",CCDPonderScenes::pumping,"booster_pump");
        scene(helper,"pumping",CCDPonderScenes::pumpVariants,"turning_booster_pump","vertical_turning_booster_pump","redstone_fluid_pump","bidirectional_fluid_pump","variable_flow_pump","portable_fluid_pump");
        scene(helper,"tanks",CCDPonderScenes::buffering,"buffer_pump","creative_delivery_fluid_tank");
        scene(helper,"pumping",CCDPonderScenes::omniPumping,"omni_pump");
        scene(helper,"pumping",CCDPonderScenes::metering,"flow_meter");
        scene(helper,"valves",CCDPonderScenes::redstoneValve,"redstone_fluid_valve");
        scene(helper,"tanks",CCDPonderScenes::filtering,"fluid_filter","fluid_attribute_filter");
        scene(helper,"collection",CCDPonderScenes::buckets,"copper_bucket","brass_bucket");
        scene(helper,"atomizing",CCDPonderScenes::media,"quicksand_bucket","seething_reagent_bucket","enriching_reagent_bucket","soul_stripping_reagent_bucket","explosive_slurry_bucket","resonant_solution_bucket","sculk_suspension_bucket","purifying_solution_bucket");
        scene(helper,"packaging",CCDPonderScenes::packaging,"fluid_arm_cargo");
        scene(helper,"packaging",CCDPonderScenes::ordering,"fluid_stock","fluid_gauge");
        for(var variant:MetalPackages.VARIANTS)
            helper.addStoryBoard(id(variant.id()),"packaging",CCDPonderScenes::packaging);
    }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("ccd",path); }
    private static void scene(PonderSceneRegistrationHelper<ResourceLocation> helper,String template,
        net.createmod.ponder.api.scene.PonderStoryBoard storyboard,String... items) {
        for(String item:items) helper.addStoryBoard(id(item),template,storyboard);
    }
}
