package dev.ccd.gametest;
import dev.ccd.api.FluidWorldScripts;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class RuleSnapshotPortTests {
 @GameTest(template="empty") public static void rulesKeepPriorityAndRefreshAfterRemoval(GameTestHelper h){
  try{
   FluidWorldScripts.blockInteraction("ccd:snapshot_z","minecraft:water","minecraft:glass","both",null,"minecraft:iron_block");
   FluidWorldScripts.blockInteraction("ccd:snapshot_a","minecraft:water","minecraft:glass","both",null,"minecraft:gold_block");
   var pos=new BlockPos(2,2,2);h.setBlock(pos.east(),Blocks.GLASS);h.setBlock(pos,Blocks.WATER);
   h.assertTrue(h.getBlockState(pos.east()).is(Blocks.GOLD_BLOCK),"Wrong priority");
   FluidWorldScripts.remove("ccd:snapshot_a");h.setBlock(pos,Blocks.AIR);h.setBlock(pos.east(),Blocks.GLASS);h.setBlock(pos,Blocks.WATER);
   h.assertTrue(h.getBlockState(pos.east()).is(Blocks.IRON_BLOCK),"Stale rule");h.succeed();
  }finally{FluidWorldScripts.remove("ccd:snapshot_a");FluidWorldScripts.remove("ccd:snapshot_z");}
 }
}
