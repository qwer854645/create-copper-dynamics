package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class MossificationTests {
    @GameTest(template="empty") public static void actualContactUsesVanillaTargets(GameTestHelper h){
        var floor=new BlockPos(2,1,2);
        h.setBlock(floor,Blocks.STONE);
        var cow=h.spawn(net.minecraft.world.entity.EntityType.COW,2,2,2);cow.setNoAi(true);
        cow.setPos(h.absolutePos(floor).getX()+.5,h.absolutePos(floor).getY()+1,h.absolutePos(floor).getZ()+.5);
        h.setBlock(new BlockPos(5,1,5),Blocks.STONE);
        CopperDynamics.MOSSIFICATION.get().applyEffectTick(cow,0);
        h.assertTrue(h.getBlockState(floor).is(Blocks.MOSS_BLOCK),"Touched stone did not become moss");
        h.setBlock(floor,Blocks.GLASS);
        CopperDynamics.MOSSIFICATION.get().applyEffectTick(cow,0);
        h.assertTrue(h.getBlockState(floor).is(Blocks.GLASS),"Non-replaceable block changed");
        // A wall touched by the body's side is also a target, even without an exposed top.
        var wall=new BlockPos(3,2,2);h.setBlock(wall,Blocks.STONE);h.setBlock(wall.above(),Blocks.GLASS);
        cow.setPos(h.absolutePos(wall).getX()-.3,h.absolutePos(wall).getY(),h.absolutePos(wall).getZ()+.5);
        CopperDynamics.MOSSIFICATION.get().applyEffectTick(cow,0);
        h.assertTrue(h.getBlockState(wall).is(Blocks.MOSS_BLOCK),"Side contact did not seed moss");
        h.succeed();
    }
}
