package dev.ccd.gametest;

import dev.ccd.api.AtomizerScripts;
import dev.ccd.atomizing.AtomizingRules;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class AtomizerApiTests {
    @GameTest(template="empty") public static void scriptContextCopiesAndUnknownProcessingType(GameTestHelper h){
        var input=new ItemStack(Items.DIRT,12);
        var context=new AtomizerScripts.ItemContext(input,new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,10),32,net.minecraft.core.BlockPos.ZERO,net.minecraft.resources.ResourceLocation.withDefaultNamespace("overworld"));
        context.getInput().shrink(5);context.getFluid().shrink(5);
        h.assertTrue(input.getCount()==12&&context.getInput().getCount()==12&&context.getFluid().getAmount()==10,"Script context exposes mutable cargo");
        long revision=AtomizingRules.revision();
        AtomizerScripts.itemHandler("ccd:test_copy",ctx->AtomizerScripts.output(new ItemStack(Items.CLAY_BALL,ctx.getInput().getCount())));
        h.assertTrue(AtomizingRules.revision()>revision,"Handler replacement did not invalidate processing");
        var result=AtomizerScripts.processItem("ccd:test_copy",context);result.outputs().getFirst().shrink(10);
        h.assertTrue(result.outputs().getFirst().getCount()==12,"Returned result shares mutable stacks");
        h.assertTrue(AtomizingRules.parseType("example:missing")==null,"Unknown fan type not safely ignored");
        h.succeed();
    }
}
