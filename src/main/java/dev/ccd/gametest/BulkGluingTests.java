package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.CCDFluids;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class BulkGluingTests {
    @GameTest(template="empty") public static void slimeProcessesLiveCraftingRecipes(GameTestHelper h){
        var type=CopperDynamics.BULK_GLUING.get();var input=new ItemStack(Items.PISTON,12);
        h.assertTrue(type.canProcess(input,h.getLevel()),"Piston recipe not detected");
        var output=type.process(input,h.getLevel());
        h.assertTrue(output.size()==1&&output.getFirst().is(Items.STICKY_PISTON)&&output.getFirst().getCount()==12,"Incorrect bulk result");
        h.assertTrue(input.getCount()==12,"Input mutated during recipe query");
        h.assertTrue(!type.canProcess(new ItemStack(Items.SLIME_BALL),h.getLevel()),"Slimeball self conversion allowed");
        var pos=new net.minecraft.core.BlockPos(2,2,2);h.setBlock(pos,CopperDynamics.SLIME_BLOCK.get());
        h.assertTrue(com.simibubi.create.content.kinetics.fan.processing.FanProcessingType.getAt(h.getLevel(),h.absolutePos(pos))==type,"Fan does not detect slime");
        var selected=dev.ccd.atomizing.AtomizingRules.select(h.getLevel(),new net.neoforged.neoforge.fluids.FluidStack(CCDFluids.SLIME.get(),1000));
        h.assertTrue(selected!=null&&selected.type()==type,"Atomizer does not detect slime");h.succeed();
    }
}
