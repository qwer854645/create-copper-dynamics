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
    @GameTest(template="empty",timeoutTicks=60) public static void liquidContactConvertsOnlyLegalTargets(GameTestHelper h){
        var medium=dev.ccd.compat.ProcessingMedia.ALL.stream().filter(m->m.id.equals("moss_suspension")).findFirst().orElseThrow();
        for(int x:new int[]{2,5}){
            var pos=new BlockPos(x,2,2);
            h.setBlock(pos.below(),Blocks.STONE);
            h.setBlock(pos.east(),Blocks.STONE);
            h.setBlock(pos.west(),Blocks.GLASS);
            h.setBlock(pos.north(),Blocks.GLASS);
            h.setBlock(pos.south(),Blocks.GLASS);
            h.setBlock(pos.above(),Blocks.GLASS);
            h.setBlock(pos,medium.block.get().defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL,x==2?0:4));
        }
        h.runAfterDelay(5,()->{
            for(int x:new int[]{2,5}){
                var pos=new BlockPos(x,2,2);
                h.assertTrue(h.getBlockState(pos.below()).is(Blocks.MOSS_BLOCK),"Liquid floor contact failed");
                h.assertTrue(h.getBlockState(pos.east()).is(Blocks.MOSS_BLOCK),"Liquid side contact failed");
                h.assertTrue(h.getBlockState(pos.west()).is(Blocks.GLASS),"Liquid converted an illegal target");
            }
            h.succeed();
        });
    }
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
