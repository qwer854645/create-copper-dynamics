package dev.ccd.gametest;

import dev.ccd.api.FluidWorldScripts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class ScriptFluidInteractionTests {
    @GameTest(template="empty") public static void scriptPairOverridesNativeAndSupportsFlowing(GameTestHelper h){
        var id="ccd:test_fluid_pair";
        try{
            FluidWorldScripts.interaction(id,"minecraft:lava","minecraft:water","minecraft:gold_block","minecraft:iron_block","minecraft:diamond_block");
            var pos=new BlockPos(2,2,2);h.setBlock(pos,Blocks.LAVA);h.setBlock(pos.east(),Blocks.WATER);
            h.assertTrue(h.getBlockState(pos).is(Blocks.GOLD_BLOCK),"Script rule did not override native reaction");
            var flow=new BlockPos(5,2,2);h.setBlock(flow.east(),Blocks.WATER);h.setBlock(flow,Blocks.LAVA.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL,3));
            h.assertTrue(h.getBlockState(flow).is(Blocks.IRON_BLOCK),"Flowing result not selected");
            var target=new BlockPos(2,2,5);h.setBlock(target,Blocks.WATER);
            h.assertTrue(FluidWorldScripts.downward(h.getLevel(),h.absolutePos(target),Direction.DOWN,Fluids.LAVA.defaultFluidState()),"Downward interaction missing");
            h.assertTrue(h.getBlockState(target).is(Blocks.DIAMOND_BLOCK),"Downward replaced wrong block");
            FluidWorldScripts.remove(id);
            h.assertTrue(!FluidWorldScripts.downward(h.getLevel(),h.absolutePos(target),Direction.DOWN,Fluids.LAVA.defaultFluidState()),"Removed rule still active");
            h.succeed();
        }finally{FluidWorldScripts.remove(id);}
    }
}
