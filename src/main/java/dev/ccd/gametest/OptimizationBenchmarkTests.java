package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

/** Opt-in CPU sample, never a timing assertion. Not a whole-production-line benchmark. */
@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class OptimizationBenchmarkTests {
    @GameTest(template="empty") public static void sampleFixedDeviceWorkload(GameTestHelper h){
        if(!Boolean.getBoolean("ccd.benchmark")){h.succeed();return;}
        var machines=new java.util.ArrayList<com.simibubi.create.foundation.blockEntity.SmartBlockEntity>();
        var blocks=java.util.List.of(CopperDynamics.OMNI_PUMP.get(),CopperDynamics.FAUCET.get(),CopperDynamics.FLUID_PACKAGER.get(),CopperDynamics.TANK_BURNER.get());
        for(int i=0;i<4;i++){
            var pos=new net.minecraft.core.BlockPos(i+1,2,2);h.setBlock(pos,blocks.get(i).defaultBlockState());
            machines.add((com.simibubi.create.foundation.blockEntity.SmartBlockEntity)h.getBlockEntity(pos));
        }
        for(var machine:machines){
            for(int i=0;i<100;i++)machine.tick();
            long start=System.nanoTime();
            for(int i=0;i<1000;i++)machine.tick();
            org.slf4j.LoggerFactory.getLogger("CCD-Benchmark").info("CCD_DEVICE_SAMPLE device={} idle_tick_calls=1000 elapsed_ns={}",machine.getBlockState().getBlock(),System.nanoTime()-start);
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void sampleGluingLookup(GameTestHelper h){
        if(!Boolean.getBoolean("ccd.benchmark")){h.succeed();return;}
        var type=CopperDynamics.BULK_GLUING.get();var input=new ItemStack(Items.PISTON);
        for(int i=0;i<100;i++)type.canProcess(input,h.getLevel());
        long started=System.nanoTime();
        for(int i=0;i<1000;i++)type.canProcess(input,h.getLevel());
        long elapsed=System.nanoTime()-started;
        org.slf4j.LoggerFactory.getLogger("CCD-Benchmark").info("CCD_BENCHMARK gluing_warm_queries=1000 elapsed_ns={} crafting_recipes={}",elapsed,h.getLevel().getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING).size());
        h.succeed();
    }
}
