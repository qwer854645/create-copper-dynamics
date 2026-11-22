package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class NativeEndpointTests {
    @GameTest(template="empty",timeoutTicks=180) public static void storedSourcesSupplyNativePipes(GameTestHelper h){
        var bufferPos=new BlockPos(1,2,2);var creativePos=new BlockPos(1,2,5);
        h.setBlock(bufferPos,CopperDynamics.BUFFER_PUMP.get().defaultBlockState().setValue(dev.ccd.content.BufferPumpBlock.FACING,Direction.EAST));
        h.setBlock(creativePos,CopperDynamics.CREATIVE_DELIVERY_TANK.get().defaultBlockState().setValue(dev.ccd.content.CreativeDeliveryTankBlock.FACING,Direction.EAST));
        for(var pos:java.util.List.of(bufferPos,creativePos)){
            h.setBlock(pos.east(),com.simibubi.create.AllBlocks.FLUID_PIPE.get().getAxisState(Direction.Axis.X));
            h.setBlock(pos.east(2),com.simibubi.create.AllBlocks.FLUID_TANK.getDefaultState());
        }
        var buffer=(dev.ccd.content.BufferPumpBlockEntity)h.getBlockEntity(bufferPos);
        var creative=(dev.ccd.content.CreativeDeliveryTankBlockEntity)h.getBlockEntity(creativePos);
        h.runAfterDelay(10,()->{
            var water=new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000);
            buffer.buffer.fill(water,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            buffer.setSpeed(64);buffer.onSpeedChanged(0);creative.select(water);
        });
        h.runAfterDelay(80,()->{
            for(var pos:java.util.List.of(bufferPos,creativePos))h.assertTrue(((com.simibubi.create.content.fluids.tank.FluidTankBlockEntity)h.getBlockEntity(pos.east(2))).getTankInventory().getFluidAmount()>0,"Native pipe source failed: "+pos);
            h.succeed();
        });
    }
}
