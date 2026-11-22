package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.content.FlowMeterBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class FlowMeterTests {
    @GameTest(template="empty",timeoutTicks=160) public static void measuresActualTransferAndStops(GameTestHelper h){
        var sourcePos=new BlockPos(1,2,2);var pumpPos=new BlockPos(2,2,2);var meterPos=new BlockPos(3,2,2);var sinkPos=new BlockPos(4,2,2);
        h.setBlock(sourcePos,AllBlocks.FLUID_TANK.getDefaultState());
        h.setBlock(pumpPos,CopperDynamics.BOOSTER_PUMP.get().defaultBlockState().setValue(PumpBlock.FACING,Direction.EAST));
        h.setBlock(meterPos,CopperDynamics.FLOW_METER.get().getAxisState(Direction.Axis.X).setValue(dev.ccd.content.FlowMeterBlock.FACING,Direction.NORTH));
        h.setBlock(sinkPos,AllBlocks.FLUID_TANK.getDefaultState());
        var source=(FluidTankBlockEntity)h.getBlockEntity(sourcePos);
        var pump=(PumpBlockEntity)h.getBlockEntity(pumpPos);
        h.runAfterDelay(10,()->{source.getTankInventory().fill(new FluidStack(Fluids.WATER,8000),FluidAction.EXECUTE);pump.setSpeed(64);pump.onSpeedChanged(0);pump.updatePressureChange();});
        h.runAfterDelay(60,()->{
            var sink=(FluidTankBlockEntity)h.getBlockEntity(sinkPos);
            var meter=(FlowMeterBlockEntity)h.getBlockEntity(meterPos);
            h.assertTrue(sink.getTankInventory().getFluidAmount()>0,"Pipe did not transfer water");
            h.assertTrue(dev.ccd.compat.pnp.PnPCompat.active()?meter.flowRate>0:meter.flowRate==64,"Expected actual flow, got "+meter.flowRate);
            // Full destination keeps pressure but must produce a zero reading.
            sink.getTankInventory().fill(new FluidStack(Fluids.WATER,8000),FluidAction.EXECUTE);
            h.runAfterDelay(10,()->{h.assertTrue(meter.flowRate==0,"Blocked output must read zero");h.succeed();});
        });
    }
}
