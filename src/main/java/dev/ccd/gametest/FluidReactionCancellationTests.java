package dev.ccd.gametest;

import dev.ccd.api.FluidWorldScripts;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class FluidReactionCancellationTests {
    @GameTest(template="empty") public static void cancellationBlocksOriginalAndRemovalRestoresIt(GameTestHelper h){
        var lava=new BlockPos(2,2,2);var water=lava.east();
        String id="ccd:test_cancel_lava_water";
        try{
            FluidWorldScripts.cancelInteraction(id,"minecraft:lava","minecraft:water");
            h.setBlock(lava,Blocks.LAVA);h.setBlock(water,Blocks.WATER);
            var level=h.getLevel();var absolute=h.absolutePos(lava);
            h.assertTrue(!net.neoforged.neoforge.fluids.FluidInteractionRegistry.canInteract(level,absolute),"Cancelled lava reaction still executed");
            h.assertTrue(h.getBlockState(lava).is(Blocks.LAVA),"Cancellation replaced source");
            h.assertTrue(FluidWorldScripts.cancels(level,h.absolutePos(water),absolute,level.getFluidState(h.absolutePos(water))),"Pair cancellation was not symmetric");
            FluidWorldScripts.remove(id);
            h.assertTrue(net.neoforged.neoforge.fluids.FluidInteractionRegistry.canInteract(level,absolute),"Original reaction did not resume");
            h.assertTrue(h.getBlockState(lava).is(Blocks.OBSIDIAN),"Original result changed");
        }finally{FluidWorldScripts.remove(id);}
        h.succeed();
    }
}
