package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.api.FluidWorldScripts;
import dev.ccd.content.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class RuleSnapshotPortTests {
    @GameTest(template="empty") public static void rulesKeepPriorityAndRefreshAfterRemoval(GameTestHelper h){
        var a="ccd:snapshot_a";var z="ccd:snapshot_z";
        try{
            FluidWorldScripts.blockInteraction(z,"minecraft:water","minecraft:glass","both",null,"minecraft:iron_block");
            FluidWorldScripts.blockInteraction(a,"minecraft:water","minecraft:glass","both",null,"minecraft:gold_block");
            var pos=new BlockPos(2,2,2);h.setBlock(pos.east(),Blocks.GLASS);h.setBlock(pos,Blocks.WATER);
            h.assertTrue(h.getBlockState(pos.east()).is(Blocks.GOLD_BLOCK),"Snapshot changed sorted priority");
            FluidWorldScripts.remove(a);h.setBlock(pos,Blocks.AIR);h.setBlock(pos.east(),Blocks.GLASS);h.setBlock(pos,Blocks.WATER);
            h.assertTrue(h.getBlockState(pos.east()).is(Blocks.IRON_BLOCK),"Removed rule remained in snapshot");h.succeed();
        }finally{FluidWorldScripts.remove(a);FluidWorldScripts.remove(z);}
    }
    @GameTest(template="empty") public static void reusedPortsStillRespectCurrentMode(GameTestHelper h){
        var pos=new BlockPos(2,2,2);h.setBlock(pos,CopperDynamics.OMNI_PUMP.get().defaultBlockState().setValue(OmniPumpBlock.PORTS.get(Direction.EAST),OmniPumpBlock.INPUT));
        var pump=(OmniPumpBlockEntity)h.getBlockEntity(pos);var input=pump.handler(Direction.EAST);
        h.assertTrue(input==pump.handler(Direction.EAST),"Input port was allocated repeatedly");
        h.getLevel().setBlockAndUpdate(h.absolutePos(pos),pump.getBlockState().setValue(OmniPumpBlock.PORTS.get(Direction.EAST),OmniPumpBlock.OUTPUT));
        h.assertTrue(input.fill(new FluidStack(Fluids.WATER,100),FluidAction.EXECUTE)==0,"Stale input accepted after mode change");
        var output=pump.handler(Direction.EAST);h.assertTrue(output!=input&&output==pump.handler(Direction.EAST),"Output port cache mismatch");
        h.getLevel().setBlockAndUpdate(h.absolutePos(pos),pump.getBlockState().setValue(OmniPumpBlock.PORTS.get(Direction.EAST),OmniPumpBlock.CLOSED));
        h.assertTrue(pump.handler(Direction.EAST)==null&&output.drain(100,FluidAction.EXECUTE).isEmpty(),"Closed port remained accessible");h.succeed();
    }
}
