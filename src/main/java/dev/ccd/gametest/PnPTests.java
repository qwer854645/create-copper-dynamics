package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.compat.pnp.PnPCompat;
import dev.ccd.content.TurningBoosterPumpBlock;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
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
public class PnPTests {
    @GameTest(template="empty",timeoutTicks=200)
    public static void creativePipeRespectsWorldMode(GameTestHelper h){
        if(!PnPCompat.active()){h.succeed();return;}
        var pos=new BlockPos(1,2,2);var mouth=pos.east(2);
        h.setBlock(pos,CopperDynamics.CREATIVE_DELIVERY_TANK.get().defaultBlockState().setValue(dev.ccd.content.CreativeDeliveryTankBlock.FACING,Direction.EAST));
        h.setBlock(pos.east(),AllBlocks.FLUID_PIPE.get().getAxisState(Direction.Axis.X));
        var tank=(dev.ccd.content.CreativeDeliveryTankBlockEntity)h.getBlockEntity(pos);
        h.runAfterDelay(10,()->tank.select(new FluidStack(Fluids.WATER,1000)));
        h.runAfterDelay(60,()->{
            h.assertTrue(h.getLevel().getFluidState(h.absolutePos(mouth)).isEmpty(),"Containers mode spilled through PnP");
            tank.worldOutput.setValue(1);tank.invalidatePressure();
        });
        h.runAfterDelay(150,()->{
            h.assertTrue(h.getLevel().getFluidState(h.absolutePos(mouth)).isSource(),"World mode did not spill through PnP");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=160)
    public static void turningPumpMovesAroundCorner(GameTestHelper h){
        if(!PnPCompat.active()){h.succeed();return;}
        var pos=new BlockPos(3,2,3);var source=pos.above(2);var sink=pos.east(2);
        h.setBlock(source,AllBlocks.FLUID_TANK.getDefaultState());
        h.setBlock(sink,AllBlocks.FLUID_TANK.getDefaultState());
        h.setBlock(pos.above(),AllBlocks.FLUID_PIPE.get().getAxisState(Direction.Axis.Y));
        h.setBlock(pos.east(),AllBlocks.FLUID_PIPE.get().getAxisState(Direction.Axis.X));
        h.setBlock(pos,CopperDynamics.TURNING_BOOSTER_PUMP.get().defaultBlockState().setValue(TurningBoosterPumpBlock.FACING,Direction.EAST));
        var pump=(PumpBlockEntity)h.getBlockEntity(pos);
        h.runAfterDelay(10,()->{
            ((FluidTankBlockEntity)h.getBlockEntity(source)).getTankInventory().fill(new FluidStack(Fluids.WATER,8000),FluidAction.EXECUTE);
            pump.setSpeed(64);pump.onSpeedChanged(0);
        });
        h.runAfterDelay(100,()->{
            h.assertTrue(((FluidTankBlockEntity)h.getBlockEntity(sink)).getTankInventory().getFluidAmount()>0,"Turning pump failed to deliver around corner");
            int total=((FluidTankBlockEntity)h.getBlockEntity(source)).getTankInventory().getFluidAmount()+((FluidTankBlockEntity)h.getBlockEntity(sink)).getTankInventory().getFluidAmount()+PnPCompat.stored(h.getLevel(),h.absolutePos(pos.above()))+PnPCompat.stored(h.getLevel(),h.absolutePos(pos.east()));
            h.assertTrue(total==8000,"Turning pump did not conserve fluid: "+total);h.succeed();
        });
    }
}
