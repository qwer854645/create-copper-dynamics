package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.content.SmartFaucetBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class SmartFaucetModeTests {
    @GameTest(template="empty") public static void outputFilterPreflightAndPersistence(GameTestHelper h){
        var pos=new BlockPos(2,2,2);h.setBlock(pos,CopperDynamics.SMART_FAUCET.get());
        var faucet=(SmartFaucetBlockEntity)h.getBlockEntity(pos);
        var filter=faucet.getBehaviour(com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour.TYPE);
        var water=new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000);
        var bottle=new ItemStack(Items.GLASS_BOTTLE);
        h.assertTrue(!faucet.outputMode(),"Default fluid mode changed");
        faucet.setOutputMode(true);filter.setFilter(new ItemStack(Items.DIAMOND));
        h.assertTrue(!faucet.acceptsFillingOutput(bottle,water,250),"Wrong output filter accepted bottle");
        h.assertTrue(water.getAmount()==1000&&bottle.getCount()==1,"Preflight consumed inputs");
        filter.setFilter(net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION,net.minecraft.world.item.alchemy.Potions.WATER));
        h.assertTrue(faucet.acceptsFillingOutput(bottle,water,250),"Water bottle target rejected");
        var saved=faucet.saveWithFullMetadata(h.getLevel().registryAccess());
        faucet.setOutputMode(false);faucet.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(faucet.outputMode(),"Mode did not survive save/load");
        filter.setFilter(ItemStack.EMPTY);
        h.assertTrue(faucet.acceptsFillingOutput(bottle,water,250),"Empty filter blocked output");
        h.succeed();
    }
}
