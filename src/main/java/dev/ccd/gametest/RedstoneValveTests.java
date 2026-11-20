package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.content.RedstoneFluidValveBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class RedstoneValveTests {
    @GameTest(template="empty") public static void signalLocksValveAndRemovalReopens(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);
        h.setBlock(pos,CopperDynamics.REDSTONE_FLUID_VALVE.get().defaultBlockState());
        h.runAfterDelay(3,()->{
            h.assertTrue(h.getBlockState(pos).getValue(RedstoneFluidValveBlock.ENABLED),"Unpowered valve must open");
            h.setBlock(pos.above(),Blocks.REDSTONE_BLOCK);
            h.runAfterDelay(3,()->{
                h.assertTrue(!h.getBlockState(pos).getValue(RedstoneFluidValveBlock.ENABLED),"Powered valve must close");
                h.setBlock(pos.above(),Blocks.AIR);
                h.runAfterDelay(3,()->{
                    h.assertTrue(h.getBlockState(pos).getValue(RedstoneFluidValveBlock.ENABLED),"Valve must reopen after signal removed");
                    h.succeed();
                });
            });
        });
    }
}
