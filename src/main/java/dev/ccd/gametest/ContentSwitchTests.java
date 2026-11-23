package dev.ccd.gametest;

import dev.ccd.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class ContentSwitchTests {
    @GameTest(template="empty") public static void independentSwitchRetainsSavedContent(GameTestHelper h) {
        var pos=new BlockPos(2,1,2);
        h.setBlock(pos,CopperDynamics.BUFFER_PUMP.get());
        var pump=(dev.ccd.content.BufferPumpBlockEntity)h.getBlockEntity(pos);
        pump.buffer.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),
            net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        var setting=CCDFeatures.CONTENT.get("buffer_pump");
        boolean before=setting.get();
        try {
            setting.set(false);
            setting.clearCache();
            h.assertTrue(!CCDFeatures.enabled(pump.getBlockState()),"Feature identity did not disable buffer");
            h.getLevel().invalidateCapabilities(h.absolutePos(pos));
            pump.tick();
            h.assertTrue(h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                h.absolutePos(pos),null)==null,"Disabled buffer still exposes machine capability");
            h.assertTrue(pump.buffer.getFluidAmount()==1000,"Disabled device lost saved contents");
            h.assertTrue(CCDFeatures.enabled("fluid_packager"),"Independent setting disabled another feature");
            h.assertTrue(!new FeatureEnabledCondition("buffer_pump").test(net.neoforged.neoforge.common.conditions.ICondition.IContext.EMPTY),"Disabled recipe condition passed");
        } finally {setting.set(before);setting.clearCache();h.getLevel().invalidateCapabilities(h.absolutePos(pos));}
        h.assertTrue(pump.buffer.getFluidAmount()==1000,"Re-enabling changed stored fluid");
        h.succeed();
    }
    @GameTest(template="empty") public static void defaultFeatureMappings(GameTestHelper h) {
        h.assertTrue(!CCDFeatures.REBIRTH_NAME.getDefault(),"Easter egg must be off by default");
        h.assertTrue(CCDFeatures.CONTENT.size()==45,"Content switch coverage changed");
        for(String standalone:java.util.List.of("seething_reagent","powder_snow","quicksand"))
            h.assertTrue(CCDFeatures.feature(standalone).equals(standalone)&&CCDFeatures.CONTENT.containsKey(standalone),"Standalone fluid lost its switch");
        for(var entry:CCDFeatures.CONTENT.entrySet())h.assertTrue(entry.getValue().getDefault()==!CCDFeatures.DEFAULT_DISABLED.contains(entry.getKey()),"Incorrect content default: "+entry.getKey());
        h.assertTrue(CCDFeatures.feature("flowing_smoking_fluid").equals("base_fluids"),"Flowing identity mismatch");
        for(var entry:CCDFeatures.FLUID_CATEGORIES.entrySet()) {
            h.assertTrue(CCDFeatures.feature(entry.getKey()+"_bucket").equals(entry.getValue()),"Bucket category mismatch");
            h.assertTrue(CCDFeatures.feature("flowing_"+entry.getKey()).equals(entry.getValue()),"Fluid category mismatch");
        }
        h.assertTrue(CCDFeatures.feature("brass_package_10x8").equals("composite_package"),"Package variant mismatch");
        h.succeed();
    }
    @GameTest(template="empty") public static void configuredSmokingRecipeAndTagAgree(GameTestHelper h) {
        boolean enabled=CCDFeatures.enabled("smoking_fluid");
        boolean recipe=h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("ccd:mixing/smoking_fluid")).isPresent();
        var medium=dev.ccd.compat.ProcessingMedia.ALL.stream().filter(m->m.id.equals("smoking_fluid")).findFirst().orElseThrow();
        var pos=new BlockPos(2,1,2);h.setBlock(pos,medium.block.get());
        boolean tagged=com.simibubi.create.content.kinetics.fan.processing.FanProcessingType.getAt(h.getLevel(),h.absolutePos(pos))
            ==com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes.SMOKING;
        h.assertTrue(recipe==enabled,"Recipe does not respect loaded content config");
        h.assertTrue(tagged==enabled,"Catalyst tag does not respect loaded content config");
        System.out.println("CCD_CONTENT_SWITCH smoking="+enabled+" recipe="+recipe+" tag="+tagged);
        h.succeed();
    }
    @GameTest(template="empty") public static void fluidOriginCategoryControlsAllForms(GameTestHelper h) {
        var setting=CCDFeatures.CONTENT.get("base_fluids");boolean previous=setting.get();
        try {
            setting.set(false);setting.clearCache();
            for(String fluid:java.util.List.of("haunting","slime","smoking_fluid")) {
                h.assertTrue(!CCDFeatures.enabled(fluid)&&!CCDFeatures.enabled("flowing_"+fluid)
                    &&!CCDFeatures.enabled(fluid+"_bucket"),"Category did not disable all forms: "+fluid);
            }
            for(String fluid:java.util.List.of("seething_reagent","powder_snow","quicksand"))
                h.assertTrue(CCDFeatures.CONTENT.get(fluid).getDefault(),"Standalone fluid default changed");
            setting.set(true);setting.clearCache();
            h.assertTrue(CCDFeatures.enabled("haunting_bucket"),"Category did not re-enable bucket");
        }finally{setting.set(previous);setting.clearCache();}
        h.succeed();
    }
}
