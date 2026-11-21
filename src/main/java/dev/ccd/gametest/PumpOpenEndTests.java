package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.logistics.PumpOpenEnds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class PumpOpenEndTests {
    @GameTest(template="empty") public static void creativeModeControlsWorldOutput(GameTestHelper h){
        var pos=new BlockPos(2,2,2);
        h.setBlock(pos,CopperDynamics.CREATIVE_DELIVERY_TANK.get().defaultBlockState().setValue(dev.ccd.content.CreativeDeliveryTankBlock.FACING,Direction.NORTH));
        h.setBlock(pos.north(),net.minecraft.world.level.block.Blocks.AIR);
        var tank=(dev.ccd.content.CreativeDeliveryTankBlockEntity)h.getBlockEntity(pos);
        tank.select(new FluidStack(Fluids.WATER,1000));
        tank.tick();
        h.assertTrue(h.getLevel().getFluidState(h.absolutePos(pos.north())).isEmpty(),"Default mode spilled into world");
        tank.worldOutput.setValue(1);
        tank.tick();tank.tick();
        h.assertTrue(h.getLevel().getFluidState(h.absolutePos(pos.north())).is(Fluids.WATER),"World mode did not release water");
        h.succeed();
    }
    @GameTest(template="empty") public static void partialBucketSurvivesReloadAndPlacesWater(GameTestHelper h){
        var pos=new BlockPos(2,2,2);h.setBlock(pos,CopperDynamics.BUFFER_PUMP.get().defaultBlockState());
        var owner=h.getBlockEntity(pos);var output=h.absolutePos(pos.north());
        h.setBlock(pos.north(),net.minecraft.world.level.block.Blocks.AIR);
        var ends=new PumpOpenEnds();var handler=ends.handler(owner,output,Direction.SOUTH);
        h.assertTrue(handler!=null,"Native open end missing");
        int accepted=handler.fill(new FluidStack(Fluids.WATER,500),IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(accepted>0&&accepted<=500,"Open end rejected fluid");
        if(accepted<500)accepted+=handler.fill(new FluidStack(Fluids.WATER,500-accepted),IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(accepted==500,"Open end failed to accumulate partial bucket");
        var saved=new net.minecraft.nbt.CompoundTag();ends.write(saved,h.getLevel().registryAccess());
        var restored=new PumpOpenEnds();restored.read(saved,h.getLevel().registryAccess());
        var loaded=restored.handler(owner,output,Direction.SOUTH);
        loaded.fill(new FluidStack(Fluids.WATER,500),IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(h.getLevel().getFluidState(output).is(Fluids.WATER),"Native open end failed to place saved bucket");
        h.succeed();
    }
}
