package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.content.*;
import dev.ccd.logistics.DeviceMeasurements;
import com.simibubi.create.AllBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class ActiveDeviceMeasurementTests {
    @GameTest(template="empty",timeoutTicks=450) public static void measureScheduledActiveDevices(GameTestHelper h){
        if(!DeviceMeasurements.ENABLED){h.succeed();return;}
        // Four isolated lanes in one loaded test structure; normal world tick ordering.
        var pumpPos=new BlockPos(2,2,1);var faucetPos=new BlockPos(2,2,3);
        var packPos=new BlockPos(2,2,5);var burnerPos=new BlockPos(2,2,7);
        for(var pos:java.util.List.of(pumpPos,faucetPos,packPos)){
            h.setBlock(pos.west(),AllBlocks.FLUID_TANK.getDefaultState());
        }
        h.setBlock(pumpPos.east(),AllBlocks.FLUID_TANK.getDefaultState());
        h.setBlock(faucetPos.below(),AllBlocks.FLUID_TANK.getDefaultState());
        h.setBlock(pumpPos,CopperDynamics.OMNI_PUMP.get().defaultBlockState().setValue(OmniPumpBlock.PORTS.get(Direction.WEST),OmniPumpBlock.INPUT).setValue(OmniPumpBlock.PORTS.get(Direction.EAST),OmniPumpBlock.OUTPUT));
        h.setBlock(faucetPos,CopperDynamics.FAUCET.get().defaultBlockState().setValue(FaucetBlock.FACING,Direction.EAST).setValue(FaucetBlock.OPEN,true));
        h.setBlock(packPos,CopperDynamics.FLUID_PACKAGER.get().defaultBlockState().setValue(com.simibubi.create.content.logistics.packager.PackagerBlock.FACING,Direction.EAST));
        h.setBlock(burnerPos,CopperDynamics.TANK_BURNER.get().defaultBlockState());
        var pump=(OmniPumpBlockEntity)h.getBlockEntity(pumpPos);pump.setSpeed(64);pump.onSpeedChanged(0);
        var pack=(FluidPackagerBlockEntity)h.getBlockEntity(packPos);
        var burner=(TankBlazeBurnerBlockEntity)h.getBlockEntity(burnerPos);
        DeviceMeasurements.watch(java.util.List.of(pump,pack,burner,h.getBlockEntity(faucetPos)));
        final int[] packaged={0};
        for(int tick=10;tick<=310;tick+=10){int scheduled=tick;
            h.runAfterDelay(tick,()->{
                pump.setSpeed(64);pump.onSpeedChanged(0);
                for(var pos:java.util.List.of(pumpPos,faucetPos,packPos)){
                    var tank=(com.simibubi.create.content.fluids.tank.FluidTankBlockEntity)h.getBlockEntity(pos.west());
                    tank.getTankInventory().fill(new FluidStack(Fluids.WATER,4000),FluidAction.EXECUTE);
                }
                for(var pos:java.util.List.of(pumpPos.east(),faucetPos.below()))((com.simibubi.create.content.fluids.tank.FluidTankBlockEntity)h.getBlockEntity(pos)).getTankInventory().drain(8000,FluidAction.EXECUTE);
                burner.tank.fill(new FluidStack(Fluids.LAVA,1000),FluidAction.EXECUTE);
                pack.attemptToSend(null);
                if(!pack.heldBox.isEmpty()){packaged[0]+=FluidPackageItem.contents(pack.heldBox).getAmount();pack.heldBox=net.minecraft.world.item.ItemStack.EMPTY;}
            });
        }
        h.runAfterDelay(330,()->{
            h.assertTrue(packaged[0]>0,"Measurement packager never produced cargo");
            h.assertTrue(burner.getRemainingBurnTime()>0,"Measurement burner inactive");
            org.slf4j.LoggerFactory.getLogger("CCD-Benchmark").info("CCD_ACTIVE_DEVICE_SAMPLE duration_ticks=330 packaged_mb={} {}",packaged[0],DeviceMeasurements.report());
            DeviceMeasurements.stop();
            h.succeed();
        });
    }
}
