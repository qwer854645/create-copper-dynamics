package dev.ccd.client.ponder;

import dev.ccd.CopperDynamics;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public final class CCDPonderPlugin implements PonderPlugin {
    @Override public String getModId() { return CopperDynamics.ID; }
    @Override public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        scene(helper,"tanks",CCDPonderScenes::tankBurner,"tank_blaze_burner");
        scene(helper,"packaging",CCDPonderScenes::packaging,"fluid_packager");
        scene(helper,"hatches",CCDPonderScenes::hatches,"fluid_hatch","smart_fluid_hatch");
        scene(helper,"access",CCDPonderScenes::access,"fluid_access_port","fluid_bridge");
        scene(helper,"tanks",CCDPonderScenes::tanks,"smart_fluid_tank","horizontal_smart_fluid_tank");
        scene(helper,"faucets",CCDPonderScenes::faucets,"faucet","smart_faucet");
        scene(helper,"atomizing",CCDPonderScenes::atomizing,"fluid_atomizer");
        scene(helper,"atomizing",CCDPonderScenes::outputFiltering,"fluid_atomizer");
        scene(helper,"hatches",CCDPonderScenes::fluidArm,"fluid_arm");
        scene(helper,"packaging",CCDPonderScenes::repackaging,"fluid_repackager");
        scene(helper,"valves",CCDPonderScenes::valves,"three_way_valve");
        scene(helper,"collection",CCDPonderScenes::collection,"copper_collection_grate","smart_collection_grate");
        scene(helper,"collection",CCDPonderScenes::waterSupply,"copper_water_supply");
        scene(helper,"collection",CCDPonderScenes::waterBasin,"copper_water_basin");
        scene(helper,"transfer",CCDPonderScenes::transfer,"fluid_transmitter");
        scene(helper,"pumping",CCDPonderScenes::pumping,"booster_pump");
        scene(helper,"pumping",CCDPonderScenes::turningPump,"turning_booster_pump");
        scene(helper,"pumping",CCDPonderScenes::verticalTurningPump,"vertical_turning_booster_pump");
        scene(helper,"pumping",CCDPonderScenes::redstonePump,"redstone_fluid_pump");
        scene(helper,"pumping",CCDPonderScenes::bidirectionalPump,"bidirectional_fluid_pump");
        scene(helper,"pumping",CCDPonderScenes::portablePump,"portable_fluid_pump");
        scene(helper,"tanks",CCDPonderScenes::buffering,"buffer_pump");
        scene(helper,"tanks",CCDPonderScenes::creativeSupply,"creative_delivery_fluid_tank");
        scene(helper,"pumping",CCDPonderScenes::omniPumping,"omni_pump");
        scene(helper,"pumping",CCDPonderScenes::metering,"flow_meter");
        scene(helper,"valves",CCDPonderScenes::redstoneValve,"redstone_fluid_valve");
        scene(helper,"tanks",CCDPonderScenes::filtering,"fluid_filter","fluid_attribute_filter");
        scene(helper,"collection",CCDPonderScenes::buckets,"copper_bucket","brass_bucket");
        scene(helper,"packaging",CCDPonderScenes::ordering,"fluid_gauge");
    }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("ccd",path); }
    private static void scene(PonderSceneRegistrationHelper<ResourceLocation> helper,String template,
        net.createmod.ponder.api.scene.PonderStoryBoard storyboard,String... items) {
        for(String item:items) if(dev.ccd.CCDFeatures.enabled(item))helper.addStoryBoard(id(item),template,storyboard);
    }
}
