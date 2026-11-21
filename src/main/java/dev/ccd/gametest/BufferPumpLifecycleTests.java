package dev.ccd.gametest;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import dev.ccd.CopperDynamics;
import dev.ccd.content.BufferPumpBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class BufferPumpLifecycleTests {
    @GameTest(template="empty") public static void survivalBucketFillsBufferAndReturnsEmptyBucket(GameTestHelper h){
        var pos=new BlockPos(2,2,2);h.setBlock(pos,CopperDynamics.BUFFER_PUMP.get().defaultBlockState());
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.getInventory().clearContent();
        var stack=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
        var absolute=h.absolutePos(pos);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(absolute).add(0,0,.5),Direction.SOUTH,absolute,false);
        var state=h.getBlockState(pos);
        var result=state.useItemOn(stack,h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        var pump=(BufferPumpBlockEntity)h.getBlockEntity(pos);
        h.assertTrue(result.consumesAction()&&pump.buffer.getFluidAmount()==1000,"Survival bucket did not fill buffer");
        h.assertTrue(player.getMainHandItem().is(net.minecraft.world.item.Items.BUCKET),"Survival fluid was duplicated or bucket lost");
        player.getInventory().clearContent();h.succeed();
    }
    @GameTest(template="empty") public static void storagePumpHasNoTransparentTransport(GameTestHelper h){
        var pos=new BlockPos(2,2,2);
        h.setBlock(pos,CopperDynamics.BUFFER_PUMP.get().defaultBlockState());
        h.runAfterDelay(3,()->{
            var pump=(BufferPumpBlockEntity)h.getBlockEntity(pos);
            h.assertTrue(pump.getBehaviour(FluidTransportBehaviour.TYPE)==null,"Buffer pump retained crashing native transport");
            for(var side:Direction.values())pump.updatePipesOnSide(side);
            pump.updatePressureChange();
            var fluid=new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000);
            pump.buffer.fill(fluid,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            var saved=pump.saveWithFullMetadata(h.getLevel().registryAccess());
            var restored=new BufferPumpBlockEntity(pump.getBlockPos(),pump.getBlockState());
            restored.setLevel(h.getLevel());restored.loadWithComponents(saved,h.getLevel().registryAccess());
            restored.initialize();restored.tick();
            h.assertTrue(restored.buffer.getFluidAmount()==1000,"Storage lost during transport lifecycle fix");
            h.succeed();
        });
    }
}
