package dev.ccd.gametest;

import dev.ccd.api.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class KubeJSIntegrationProbeTests {
    @GameTest(template="empty") public static void realRhinoCallbacksAndRules(GameTestHelper h){
        if(!Boolean.getBoolean("ccd.kjsProbe")){h.succeed();return;}
        var context=new AtomizerScripts.ItemContext(new ItemStack(Items.DIRT,7),net.neoforged.neoforge.fluids.FluidStack.EMPTY,32,BlockPos.ZERO,h.getLevel().dimension().location());
        var result=AtomizerScripts.processItem("ccd_probe:item",context);
        h.assertTrue(result!=null&&result.outputs().getFirst().is(Items.CLAY_BALL)&&result.outputs().getFirst().getCount()==7,"Real JS item callback failed");
        var entity=AtomizerScripts.processEntity("ccd_probe:entity",new AtomizerScripts.EntityContext(ResourceLocation.withDefaultNamespace("cow"),java.util.UUID.randomUUID(),10,net.neoforged.neoforge.fluids.FluidStack.EMPTY,32,BlockPos.ZERO,h.getLevel().dimension().location()));
        h.assertTrue(entity!=null&&!entity.effects().isEmpty(),"Real JS entity callback failed");
        h.assertTrue(AtomizerScripts.processItem("ccd_probe:error",context)==null,"JS error did not preserve failure contract");
        var pos=new BlockPos(2,2,2);h.setBlock(pos.east(),Blocks.GLASS);h.setBlock(pos,Blocks.WATER);
        h.assertTrue(h.getBlockState(pos.east()).is(Blocks.DIAMOND_BLOCK),"Real JS block rule failed");
        h.setBlock(pos,Blocks.AIR);h.setBlock(pos.east(),Blocks.WATER);h.setBlock(pos,Blocks.LAVA);
        h.assertTrue(h.getBlockState(pos).is(Blocks.GOLD_BLOCK),"Real JS pair rule failed");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=600) public static void realServerReloadRestoresScriptBindings(GameTestHelper h){
        if(!Boolean.getBoolean("ccd.kjsProbe")){h.succeed();return;}
        h.runAfterDelay(180,()->{
            var server=h.getLevel().getServer();
            var reload=server.reloadResources(server.getPackRepository().getSelectedIds());
            h.succeedWhen(()->{
                h.assertTrue(reload.isDone(),"Waiting for real resource reload");
                h.assertTrue(!reload.isCompletedExceptionally(),"Resource reload failed");
                var result=AtomizerScripts.processItem("ccd_probe:item",new AtomizerScripts.ItemContext(new ItemStack(Items.DIRT,3),net.neoforged.neoforge.fluids.FluidStack.EMPTY,32,BlockPos.ZERO,h.getLevel().dimension().location()));
                h.assertTrue(result!=null&&result.outputs().getFirst().getCount()==3,"JS callback missing after reload");
            });
        });
    }
}
