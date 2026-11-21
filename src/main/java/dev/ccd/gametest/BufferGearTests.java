package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class BufferGearTests {
    @GameTest(template="empty") public static void heldBufferPreviewsAndPlacesAtCogwheel(GameTestHelper h){
        var pos=new BlockPos(3,3,3);var absolute=h.absolutePos(pos);
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
        var stack=new net.minecraft.world.item.ItemStack(CopperDynamics.BUFFER_PUMP_ITEM.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);player.setShiftKeyDown(false);
        var helper=new dev.ccd.content.BufferPumpPlacementHelper();
        for(var axis:Direction.Axis.values()){
            var state=com.simibubi.create.AllBlocks.COGWHEEL.getDefaultState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS,axis);
            h.setBlock(pos,state);
            for(var side:Direction.values()){
                var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(absolute).add(net.minecraft.world.phys.Vec3.atLowerCornerOf(side.getNormal()).scale(.5)),side,absolute,false);
                var offset=helper.getOffset(player,h.getLevel(),state,absolute,hit);
                if(side.getAxis()==axis){h.assertTrue(!offset.isSuccessful(),"Preview offered at shaft end");continue;}
                h.assertTrue(offset.isSuccessful()&&offset.hasGhostState(),"Buffer preview missing");
                h.assertTrue(offset.getGhostState().getValue(com.simibubi.create.content.fluids.pump.PumpBlock.FACING).getAxis()==axis,"Preview gear axis mismatch");
                var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
                h.assertTrue(CopperDynamics.BUFFER_PUMP_ITEM.get().onItemUseFirst(stack,context).consumesAction(),"Helper placement failed");
                h.assertTrue(h.getLevel().getBlockState(offset.getBlockPos()).is(CopperDynamics.BUFFER_PUMP.get()),"Placed wrong block");
                h.getLevel().setBlockAndUpdate(offset.getBlockPos(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                stack.setCount(1);
            }
            h.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR);
        }
        player.getInventory().clearContent();h.succeed();
    }
    @GameTest(template="empty") public static void twoBuffersMeshAtMatchingAxes(GameTestHelper h){
        var pos=new BlockPos(3,3,3);
        for(var front:Direction.values())for(var side:Direction.values()){
            var state=CopperDynamics.BUFFER_PUMP.get().defaultBlockState().setValue(com.simibubi.create.content.fluids.pump.PumpBlock.FACING,front);
            h.setBlock(pos,state);h.setBlock(pos.relative(side),state);
            var a=(com.simibubi.create.content.kinetics.base.KineticBlockEntity)h.getBlockEntity(pos);
            var b=(com.simibubi.create.content.kinetics.base.KineticBlockEntity)h.getBlockEntity(pos.relative(side));
            boolean expected=side.getAxis()!=front.getAxis();
            h.assertTrue(com.simibubi.create.content.kinetics.RotationPropagator.isConnected(a,b)==expected,"Buffer pair connection wrong: "+front+" / "+side);
            if(expected){
                try{
                    var modifier=com.simibubi.create.content.kinetics.RotationPropagator.class.getDeclaredMethod("getRotationSpeedModifier",com.simibubi.create.content.kinetics.base.KineticBlockEntity.class,com.simibubi.create.content.kinetics.base.KineticBlockEntity.class);
                    modifier.setAccessible(true);
                    h.assertTrue((float)modifier.invoke(null,a,b)==-1f,"Buffer pair must mesh at opposite 1:1 rotation");
                }catch(ReflectiveOperationException exception){throw new RuntimeException(exception);}
                var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
                var stack=new net.minecraft.world.item.ItemStack(CopperDynamics.BUFFER_PUMP_ITEM.get());
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
                h.setBlock(pos.relative(side),net.minecraft.world.level.block.Blocks.AIR);
                var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(a.getBlockPos()),side,a.getBlockPos(),false);
                var context=new net.minecraft.world.item.context.BlockPlaceContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,stack,hit);
                var placed=CopperDynamics.BUFFER_PUMP.get().getStateForPlacement(context);
                h.assertTrue(placed.getValue(com.simibubi.create.content.fluids.pump.PumpBlock.FACING)==front,"Placement changed matching buffer gear axis");
                player.getInventory().clearContent();
            }
            h.setBlock(pos.relative(side),net.minecraft.world.level.block.Blocks.AIR);h.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR);
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void bufferMeshesWithNativeCogwheels(GameTestHelper h){
        var pos=new BlockPos(3,3,3);
        for(var front:Direction.values()){
            var state=CopperDynamics.BUFFER_PUMP.get().defaultBlockState().setValue(com.simibubi.create.content.fluids.pump.PumpBlock.FACING,front);
            h.setBlock(pos,state);
            var pump=(com.simibubi.create.content.kinetics.base.KineticBlockEntity)h.getBlockEntity(pos);
            for(var side:Direction.values())if(side.getAxis()!=front.getAxis()){
                var target=pos.relative(side);
                h.setBlock(target,com.simibubi.create.AllBlocks.COGWHEEL.getDefaultState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS,front.getAxis()));
                var gear=(com.simibubi.create.content.kinetics.base.KineticBlockEntity)h.getBlockEntity(target);
                h.assertTrue(com.simibubi.create.content.kinetics.RotationPropagator.isConnected(pump,gear),"Buffer gear disconnected: "+front+" / "+side);
                h.assertTrue(com.simibubi.create.content.kinetics.RotationPropagator.isConnected(gear,pump),"Reverse gear connection missing");
                var helper=new com.simibubi.create.content.kinetics.simpleRelays.CogwheelBlockItem.IntegratedSmallCogHelper();
                h.setBlock(target,net.minecraft.world.level.block.Blocks.AIR);
                var absolute=h.absolutePos(pos);
                var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(absolute),side,absolute,false);
                var offset=helper.getOffset(h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE),h.getLevel(),state,absolute,hit);
                h.assertTrue(offset.isSuccessful(),"Buffer gear placement failed");
            }
            h.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR);
        }
        h.succeed();
    }
}
