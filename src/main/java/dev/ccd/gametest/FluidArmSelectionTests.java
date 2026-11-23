package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.content.FluidArmItem;
import dev.ccd.content.FluidHatchBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class FluidArmSelectionTests {
    @GameTest(template="empty") public static void destructionReturnsHeldFluid(GameTestHelper h) {
        var pos=new BlockPos(2,1,2);
        h.setBlock(pos,CopperDynamics.FLUID_ARM.get());
        var arm=(dev.ccd.content.FluidArmBlockEntity)h.getBlockEntity(pos);
        ((dev.ccd.mixin.FluidArmAccessor)(Object)arm).ccd$held(dev.ccd.content.FluidArmCargoItem.containing(
            new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000)));
        h.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR);
        var drops=h.getLevel().getEntitiesOfClass(com.simibubi.create.content.logistics.box.PackageEntity.class,
            new net.minecraft.world.phys.AABB(h.absolutePos(pos)).inflate(2));
        h.assertTrue(drops.stream().mapToInt(e->dev.ccd.content.FluidPackageItem.contents(e.box).getAmount()).sum()==1000,"Held fluid lost on destruction");
        h.assertTrue(drops.stream().noneMatch(e->e.box.getItem() instanceof dev.ccd.content.FluidArmCargoItem),"Internal cargo escaped");
        h.succeed();
    }
    @GameTest(template="empty") public static void rightClickCyclesLeftClickRemoves(GameTestHelper h) {
        var pos=new BlockPos(2,1,2);
        h.setBlock(pos.north(),CopperDynamics.SMART_FLUID_TANK.get());
        h.setBlock(pos,CopperDynamics.FLUID_HATCH.get().defaultBlockState().setValue(FluidHatchBlock.FACING,Direction.NORTH));
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var stack=new ItemStack(CopperDynamics.FLUID_ARM_ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var item=(FluidArmItem)stack.getItem();
        h.assertTrue(h.getBlockEntity(pos) instanceof dev.ccd.content.FluidHatchBlockEntity,"Hatch entity missing");
        var face=h.getBlockState(pos).getValue(FluidHatchBlock.FACING).getOpposite();
        var hit=new BlockHitResult(Vec3.atCenterOf(h.absolutePos(pos)),face,h.absolutePos(pos),false);
        var context=new UseOnContext(h.getLevel(),player,InteractionHand.MAIN_HAND,stack,hit);
        h.assertTrue(dev.ccd.logistics.FluidArmPoint.interactionHandler(h.getLevel(),h.absolutePos(pos),face)!=null,"Test face unavailable");
        for(int i=0;i<4;i++) {
            item.onItemUseFirst(stack,context);
            var selected=FluidArmItem.selected(stack,h.getLevel());
            h.assertTrue(selected.size()==1 && selected.getFirst().input()==(i%2==0),"Right click cycle incorrect: iteration="+i+" selection="+selected);
        }
        h.assertTrue(!item.canAttackBlock(h.getBlockState(pos),h.getLevel(),h.absolutePos(pos),player),"Selected target can be broken");
        h.assertTrue(FluidArmItem.deselect(stack,h.getLevel(),h.absolutePos(pos),player),"Left-click removal failed");
        h.assertTrue(FluidArmItem.selected(stack,h.getLevel()).isEmpty(),"Target remained selected");
        h.succeed();
    }
}
