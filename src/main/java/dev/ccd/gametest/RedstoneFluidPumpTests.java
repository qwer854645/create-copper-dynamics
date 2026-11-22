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
 @GameTest(template="empty") public static void wrenchReachesAllSixFacings(GameTestHelper h) {
  var block=CopperDynamics.REDSTONE_FLUID_PUMP.get();
  var reached=java.util.EnumSet.noneOf(Direction.class);
  var queue=new java.util.ArrayDeque<net.minecraft.world.level.block.state.BlockState>();
  queue.add(block.defaultBlockState().setValue(RedstoneFluidPumpBlock.POWERED,true));
  while(!queue.isEmpty()) {
   var state=queue.remove();if(!reached.add(state.getValue(RedstoneFluidPumpBlock.FACING)))continue;
   for(var side:Direction.values()) {
    var rotated=block.getRotatedBlockState(state,side);
    h.assertTrue(rotated.getValue(RedstoneFluidPumpBlock.POWERED),"Wrench cleared redstone state");
    h.assertTrue(block.getRotationAxis(rotated)!=rotated.getValue(RedstoneFluidPumpBlock.FACING).getAxis(),"Shaft overlaps fluid ports");
    queue.add(rotated);
   }
  }
  h.assertTrue(reached.size()==6,"Wrench cannot reach all six pump directions: "+reached);h.succeed();
 }
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
