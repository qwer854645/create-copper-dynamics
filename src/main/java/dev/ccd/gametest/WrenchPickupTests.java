package dev.ccd.gametest;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class WrenchPickupTests {
    @GameTest(template="empty")
    public static void pickupTagCoversEveryMachine(GameTestHelper h) {
        for(var block:BuiltInRegistries.BLOCK) {
            var id=BuiltInRegistries.BLOCK.getKey(block);
            if(id.getNamespace().equals("ccd") && !(block instanceof net.minecraft.world.level.block.LiquidBlock))
                h.assertTrue(com.simibubi.create.AllTags.AllBlockTags.WRENCH_PICKUP.matches(block.defaultBlockState()),
                    "Missing wrench pickup tag: "+id);
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public static void gaugeWrenchRemovesOnePanelAtATime(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.setShiftKeyDown(true);
        var pos=new BlockPos(2,2,2);var absolute=h.absolutePos(pos);
        h.setBlock(pos,dev.ccd.CopperDynamics.FLUID_GAUGE.get());
        var gauge=(dev.ccd.content.FluidGaugeBlockEntity)h.getBlockEntity(pos);
        var slots=com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot.values();
        var network=java.util.UUID.randomUUID();
        gauge.addPanel(slots[0],network);gauge.addPanel(slots[1],network);
        var wrench=AllItems.WRENCH.asStack();player.setItemInHand(InteractionHand.MAIN_HAND,wrench);
        for(int remaining=2;remaining>0;remaining--) {
            Vec3 location=null;
            var state=h.getBlockState(pos);
            // Locate an active quadrant independently of the panel's wall/floor orientation.
            for(double x:new double[]{.25,.75}) for(double y:new double[]{.25,.75}) for(double z:new double[]{.25,.75}) {
                var candidate=Vec3.atLowerCornerOf(absolute).add(x,y,z);
                var slot=com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.getTargetedSlot(absolute,state,candidate);
                if(gauge.panels.get(slot).isActive()) location=candidate;
            }
            h.assertTrue(location!=null,"No active panel found");
            var context=new UseOnContext(h.getLevel(),player,InteractionHand.MAIN_HAND,wrench,
                new BlockHitResult(location,Direction.UP,absolute,false));
            wrench.getItem().useOn(context);
            if(remaining>1) h.assertTrue(gauge.activePanels()==remaining-1 && !h.getBlockState(pos).isAir(),"Removed whole board");
            else h.assertTrue(h.getBlockState(pos).isAir(),"Last panel did not remove board");
        }
        int count=player.getInventory().items.stream().filter(s->s.is(dev.ccd.CopperDynamics.FLUID_GAUGE_ITEM.get())).mapToInt(ItemStack::getCount).sum();
        h.assertTrue(count==2,"Panel recovery count incorrect: "+count);
        h.succeed();
    }
    @GameTest(template="empty")
    public static void allMachinesCanBePickedUp(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        var pos=new BlockPos(2,2,2);
        for(var block:BuiltInRegistries.BLOCK) {
            var id=BuiltInRegistries.BLOCK.getKey(block);
            if(!id.getNamespace().equals("ccd") || block instanceof net.minecraft.world.level.block.LiquidBlock
                || id.getPath().equals("fluid_gauge")) continue;
            h.assertTrue(block instanceof IWrenchable,"Missing native wrench interface: "+id);
            h.setBlock(pos,block);
            player.getInventory().clearContent();
            var wrench=AllItems.WRENCH.asStack();
            player.setItemInHand(InteractionHand.MAIN_HAND,wrench);
            var absolute=h.absolutePos(pos);
            var hit=new BlockHitResult(Vec3.atCenterOf(absolute),Direction.UP,absolute,false);
            var context=new UseOnContext(h.getLevel(),player,InteractionHand.MAIN_HAND,wrench,hit);
            var result=wrench.getItem().useOn(context);
            h.assertTrue(result.consumesAction() && h.getBlockState(pos).isAir(),"Wrench did not remove: "+id);
            int count=player.getInventory().items.stream().filter(s->s.is(block.asItem())).mapToInt(ItemStack::getCount).sum();
            h.assertTrue(count==1,"Wrench did not return exactly one item: "+id+" count="+count);
            System.out.println("CCD_WRENCH_PICKUP "+id+" OK");
        }
        h.succeed();
    }
}
