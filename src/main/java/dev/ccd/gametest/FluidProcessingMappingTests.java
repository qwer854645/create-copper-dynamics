package dev.ccd.gametest;

import dev.ccd.api.FluidProcessingMappings;
import dev.ccd.atomizing.AtomizingRules;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class FluidProcessingMappingTests {
    @GameTest(template="empty") public static void mappingsPrioritizeReplaceAndRemove(GameTestHelper h){
        try{
            FluidProcessingMappings.register("ccd:mapping_low","#minecraft:water","create:smoking",10);
            FluidProcessingMappings.register("ccd:mapping_high","minecraft:water","create:haunting",20);
            for(var fluid:java.util.List.of(Fluids.WATER,Fluids.FLOWING_WATER))
                h.assertTrue(AtomizingRules.select(h.getLevel(),new FluidStack(fluid,1000)).type()==AllFanProcessingTypes.HAUNTING,"Fluid family mapping failed");
            FluidProcessingMappings.register("ccd:mapping_high","minecraft:water","ccd:atomizing",20);
            h.assertTrue(FluidProcessingMappings.select(new FluidStack(Fluids.WATER,1000))==AllFanProcessingTypes.SMOKING,"Self-referencing adapter should be skipped");
            FluidProcessingMappings.remove("ccd:mapping_low");
            var remaining=FluidProcessingMappings.select(new FluidStack(Fluids.WATER,1000));
            h.assertTrue(remaining==null||Boolean.getBoolean("ccd.mappingFixture")&&remaining==AllFanProcessingTypes.SPLASHING,"Removed mapping remained active");
            h.succeed();
        }finally{FluidProcessingMappings.remove("ccd:mapping_low");FluidProcessingMappings.remove("ccd:mapping_high");}
    }
    @GameTest(template="empty") public static void datapackAndConventionFixture(GameTestHelper h){
        if(!Boolean.getBoolean("ccd.mappingFixture")){h.succeed();return;}
        h.assertTrue(FluidProcessingMappings.select(new FluidStack(Fluids.WATER,1000))==AllFanProcessingTypes.SPLASHING,"JSON mapping fixture did not load");
        h.assertTrue(FluidProcessingMappings.discover(new FluidStack(Fluids.WATER,1000))==dev.ccd.CopperDynamics.BULK_GLUING.get(),"Convention tag fixture not discovered");
        h.succeed();
    }
}
