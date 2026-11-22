package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.content.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class PumpPlacementTests {
    @GameTest(template="empty") public static void pumpsAlignToEachOthersMouths(GameTestHelper h){
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
        player.setYRot(37);player.setXRot(20);
        var origin=new BlockPos(2,2,2);
        var items=java.util.List.of(CopperDynamics.BOOSTER_PUMP_ITEM.get(),CopperDynamics.TURNING_BOOSTER_PUMP_ITEM.get(),CopperDynamics.VERTICAL_TURNING_BOOSTER_PUMP_ITEM.get(),
            CopperDynamics.BIDIRECTIONAL_FLUID_PUMP_ITEM.get(),CopperDynamics.REDSTONE_FLUID_PUMP_ITEM.get(),
            CopperDynamics.PORTABLE_PUMP_ITEM.get(),CopperDynamics.VARIABLE_FLOW_PUMP_ITEM.get(),CopperDynamics.BUFFER_PUMP_ITEM.get());
        for(var face:Direction.values()){
            h.setBlock(origin,CopperDynamics.BUFFER_PUMP.get().defaultBlockState().setValue(BufferPumpBlock.FACING,face));
            var absolute=h.absolutePos(origin);
            for(BlockItem item:items){
                if(item.getBlock() instanceof PortableFluidPumpBlock&&face.getAxis().isVertical())continue;
                var stack=new ItemStack(item);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
                var hit=new BlockHitResult(Vec3.atCenterOf(absolute).add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5)),face,absolute,false);
                var context=new BlockPlaceContext(player,InteractionHand.MAIN_HAND,stack,hit);
                var state=item.getBlock().getStateForPlacement(context);
                h.assertTrue(state!=null&&PumpPlacement.mouth(state,face.getOpposite()),"Failed to align "+item+" at "+face);
            }
        }
        // Shaft/casing faces of a pump are not fluid connections.
        var closed=CopperDynamics.BUFFER_PUMP.get().defaultBlockState().setValue(BufferPumpBlock.FACING,Direction.UP);
        h.assertTrue(!PumpPlacement.mouth(closed,Direction.EAST),"Buffer casing exposed as mouth");
        player.getInventory().clearContent();h.succeed();
    }
    @GameTest(template="empty") public static void pumpsAlignToNativePipes(GameTestHelper h){
        var origin=new BlockPos(2,2,2);var face=Direction.EAST;
        h.setBlock(origin,com.simibubi.create.AllBlocks.FLUID_PIPE.get().getAxisState(Direction.Axis.X));
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
        player.setYRot(0);player.setXRot(0);
        var absolute=h.absolutePos(origin);var stack=new ItemStack(CopperDynamics.PORTABLE_PUMP_ITEM.get());
        var hit=new BlockHitResult(Vec3.atCenterOf(absolute).add(.5,0,0),face,absolute,false);
        var context=new BlockPlaceContext(player,InteractionHand.MAIN_HAND,stack,hit);
        var state=CopperDynamics.PORTABLE_PUMP.get().getStateForPlacement(context);
        h.assertTrue(PumpPlacement.mouth(state,Direction.WEST),"Portable pump ignored native pipe");h.succeed();
    }
}
