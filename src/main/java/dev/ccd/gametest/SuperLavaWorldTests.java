package dev.ccd.gametest;

import dev.ccd.compat.ProcessingMedia;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class SuperLavaWorldTests {
    @GameTest(template="empty",timeoutTicks=60) public static void lavaContactBurnsAndWaterSolidifies(GameTestHelper h){
        var lava=ProcessingMedia.ALL.getFirst();var pos=new BlockPos(2,2,2);
        h.setBlock(pos.below(),Blocks.STONE);h.setBlock(pos,lava.block.get());
        var cow=h.spawn(net.minecraft.world.entity.EntityType.COW,2.5f,2,2.5f);cow.setNoAi(true);
        cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);
        h.runAfterDelay(3,()->{
            h.assertTrue(cow.isOnFire(),"Super lava did not ignite entity");
            h.assertTrue(cow.getHealth()<=80,"Super lava did not deal increased damage: "+cow.getHealth());
            var waterPos=new BlockPos(5,2,2);h.setBlock(waterPos.below(),Blocks.STONE);h.setBlock(waterPos,lava.block.get());h.setBlock(waterPos.east(),Blocks.WATER);
            h.runAfterDelay(2,()->{h.assertTrue(h.getBlockState(waterPos).is(Blocks.OBSIDIAN),"Source did not solidify to obsidian");h.succeed();});
        });
    }
}
