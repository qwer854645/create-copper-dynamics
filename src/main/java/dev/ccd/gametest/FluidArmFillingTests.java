package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.content.*;
import dev.ccd.logistics.FluidArmPoint;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class FluidArmFillingTests {
    @GameTest(template="empty") public static void fillingFiltersAndConservesCargo(GameTestHelper h){
        var armPos=new BlockPos(2,1,2);var depotPos=armPos.south();
        h.setBlock(armPos,CopperDynamics.FLUID_ARM.get());h.setBlock(depotPos,com.simibubi.create.AllBlocks.DEPOT.getDefaultState());
        h.runAfterDelay(3,()->{
            var arm=(FluidArmBlockEntity)h.getBlockEntity(armPos);arm.setFillingMode(true);
            var depot=(com.simibubi.create.content.logistics.depot.DepotBlockEntity)h.getBlockEntity(depotPos);
            var inventory=h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,h.absolutePos(depotPos),Direction.UP);
            inventory.insertItem(0,new ItemStack(Items.GLASS_BOTTLE),false);
            var point=new FluidArmPoint(h.getLevel(),h.absolutePos(depotPos),h.getBlockState(depotPos),Direction.UP,
                com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint.Mode.DEPOSIT);
            var parcel=FluidArmCargoItem.containing(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000));
            var filter=arm.getBehaviour(com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour.TYPE);
            var filterSlot=(com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided)filter.getSlotPositioning();
            var scheduling=arm.getBehaviour(com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour.TYPE);
            var scheduleSlot=(com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided)scheduling.getSlotPositioning();
            for(var side:Direction.values()){
                boolean filtered=filterSlot.fromSide(side).shouldRender(h.getLevel(),h.absolutePos(armPos),arm.getBlockState());
                boolean scheduled=scheduleSlot.fromSide(side).shouldRender(h.getLevel(),h.absolutePos(armPos),arm.getBlockState());
                h.assertTrue(filtered==(side.getAxis()==Direction.Axis.X),"Wrong filter face");
                h.assertTrue(scheduled==(side.getAxis()==Direction.Axis.Z),"Wrong scheduling face");
                h.assertTrue(!filtered||!scheduled,"Control slots overlap");
            }
            filter.setFilter(new ItemStack(Items.DIAMOND));
            h.assertTrue(FluidPackageItem.contents(point.insert(arm,parcel,true)).getAmount()==1000,"Rejected output consumed simulated cargo");
            filter.setFilter(net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION,net.minecraft.world.item.alchemy.Potions.WATER));
            var planned=point.insert(arm,parcel,true);
            h.assertTrue(FluidPackageItem.contents(planned).getAmount()==750,"Preflight chose wrong filling cost");
            h.assertTrue(inventory.getStackInSlot(0).is(Items.GLASS_BOTTLE),"Preflight modified depot");
            var rest=point.insert(arm,parcel,false);
            h.assertTrue(FluidPackageItem.contents(rest).getAmount()==750,"Execution lost fluid remainder");
            h.assertTrue(inventory.getStackInSlot(0).is(Items.POTION),"Filling failed to produce water bottle");
            var saved=arm.saveWithFullMetadata(h.getLevel().registryAccess());arm.setFillingMode(false);
            arm.loadWithComponents(saved,h.getLevel().registryAccess());h.assertTrue(arm.fillingMode(),"Mode not persisted");
            h.succeed();
        });
    }
}
