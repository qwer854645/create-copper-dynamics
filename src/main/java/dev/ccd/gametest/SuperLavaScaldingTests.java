package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.compat.ProcessingMedia;
import dev.ccd.content.SuperLavaScalding;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class SuperLavaScaldingTests {
    @GameTest(template="empty") public static void bucketsAccumulateHeatAndCoolDown(GameTestHelper h){
        var lava=ProcessingMedia.ALL.getFirst();
        for(var item:java.util.List.of(CopperDynamics.COPPER_BUCKET.get(),CopperDynamics.BRASS_BUCKET.get())){
            var stack=new ItemStack(item);
            h.assertTrue(!SuperLavaScalding.hot(stack),"Empty bucket is hot");
            stack.set(CopperDynamics.COPPER_BUCKET_FLUID.get(),net.neoforged.neoforge.fluids.SimpleFluidContent.copyOf(new net.neoforged.neoforge.fluids.FluidStack(lava.source.get(),1)));
            h.assertTrue(SuperLavaScalding.hot(stack),"Filled metal bucket is not hot");
        }
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(lava.bucket.get()));
        for(int i=0;i<139;i++)SuperLavaScalding.tick(player);
        h.assertTrue(SuperLavaScalding.exposure(player)==139,"Offhand exposure timing wrong");
        player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        SuperLavaScalding.tick(player);
        h.assertTrue(SuperLavaScalding.exposure(player)==137,"Exposure did not cool down");
        h.succeed();
    }
}
