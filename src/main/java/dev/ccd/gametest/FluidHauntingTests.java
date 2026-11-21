package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.CCDFluids;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class FluidHauntingTests {
    @GameTest(template="empty") public static void fluidUsesNativeHauntingRecipes(GameTestHelper h){
        var type=CopperDynamics.FLUID_HAUNTING.get();
        var sand=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SAND,8);
        h.assertTrue(type.canProcess(sand,h.getLevel()),"Native haunting recipe not found");
        var result=type.process(sand,h.getLevel());
        h.assertTrue(result!=null&&result.stream().anyMatch(s->s.is(net.minecraft.world.item.Items.SOUL_SAND)),"Haunting result incorrect");
        var pos=new net.minecraft.core.BlockPos(2,2,2);h.setBlock(pos,CopperDynamics.HAUNTING_BLOCK.get());
        h.assertTrue(com.simibubi.create.content.kinetics.fan.processing.FanProcessingType.getAt(h.getLevel(),h.absolutePos(pos))==type,"Fan did not detect haunting fluid");
        var plan=dev.ccd.atomizing.AtomizingRules.select(h.getLevel(),new net.neoforged.neoforge.fluids.FluidStack(CCDFluids.HAUNTING.get(),1000));
        h.assertTrue(plan!=null&&plan.type()==com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes.HAUNTING,"Atomizer haunting mismatch");h.succeed();
    }
}
