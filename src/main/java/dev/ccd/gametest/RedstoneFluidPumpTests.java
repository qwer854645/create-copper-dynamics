package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.content.RedstoneFluidPumpBlock;
import dev.ccd.content.RedstoneFluidPumpBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class RedstoneFluidPumpTests {
 @GameTest(template="empty") public static void redstoneReversesNativePumpSides(GameTestHelper h) {
  var pos=new BlockPos(2,2,2);var block=CopperDynamics.REDSTONE_FLUID_PUMP.get();
  for(var facing:Direction.values()) {
   h.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR);
   h.setBlock(pos,block.defaultBlockState().setValue(RedstoneFluidPumpBlock.FACING,facing));
   var pump=(RedstoneFluidPumpBlockEntity)h.getBlockEntity(pos);
   h.assertTrue(!pump.isPullingOnSide(true) && pump.isPullingOnSide(false),"Unpowered roles incorrect");
   int shafts=0;
   for(var side:Direction.values()) if(block.hasShaftTowards(h.getLevel(),h.absolutePos(pos),pump.getBlockState(),side)) {
    shafts++;h.assertTrue(side.getAxis()!=facing.getAxis(),"Shaft overlaps pipe");
   }
   h.assertTrue(shafts==2,"Pump requires exactly two shaft ports");
   h.setBlock(pos.above(),net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
   block.neighborChanged(h.getBlockState(pos),h.getLevel(),h.absolutePos(pos),net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK,h.absolutePos(pos.above()),false);
   h.assertTrue(pump.getBlockState().getValue(RedstoneFluidPumpBlock.POWERED),"Signal not received");
   h.assertTrue(pump.isPullingOnSide(true) && !pump.isPullingOnSide(false),"Powered roles not reversed");
   h.setBlock(pos.above(),net.minecraft.world.level.block.Blocks.AIR);
  }
  h.succeed();
 }
}
