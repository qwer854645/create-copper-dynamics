package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.CCDFluids;
import dev.ccd.atomizing.AtomizingRules;
import dev.ccd.atomizing.SandblastingType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.fluids.FluidStack;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.AllItems;
import net.minecraft.world.item.ItemStack;

@GameTestHolder(CopperDynamics.ID)
@PrefixGameTestTemplate(false)
public final class QuicksandTests {
    @GameTest(template="empty")
    public static void sandpaperRecipesWorkInBulk(GameTestHelper h) {
        var type=new SandblastingType();
        var input=AllItems.ROSE_QUARTZ.asStack(16);
        h.assertTrue(type.canProcess(input,h.getLevel()),"Sandpaper recipe not found");
        var output=type.process(input,h.getLevel());
        h.assertTrue(output!=null && output.stream().filter(s->s.is(AllItems.POLISHED_ROSE_QUARTZ.get())).mapToInt(ItemStack::getCount).sum()==16,"Bulk sanding quantity incorrect");
        var pos=new BlockPos(1,2,1);
        h.setBlock(pos,CopperDynamics.QUICKSAND_BLOCK.get());
        h.assertTrue(FanProcessingType.getAt(h.getLevel(),h.absolutePos(pos))==SandblastingType.processingType(),"Fan cannot use quicksand");
        var rule=AtomizingRules.select(h.getLevel(),new FluidStack(CCDFluids.QUICKSAND.get(),1000));
        h.assertTrue(rule!=null && rule.type()==SandblastingType.processingType(),"Atomizer cannot use quicksand");
        h.succeed();
    }
}
