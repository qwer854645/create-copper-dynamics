package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.content.TankBlazeBurnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class BurnerLifecycleTests {
    @GameTest(template="empty",timeoutTicks=150) public static void fuelSwitchAndReloadKeepVolume(GameTestHelper h){
        var pos=new BlockPos(2,2,2);h.setBlock(pos,CopperDynamics.TANK_BURNER.get().defaultBlockState());
        var be=(TankBlazeBurnerBlockEntity)h.getBlockEntity(pos);
        var hot=dev.ccd.compat.ProcessingMedia.ALL.getFirst().source.get();
        be.tank.fill(new FluidStack(Fluids.LAVA,1500),FluidAction.EXECUTE);
        h.assertTrue(be.tank.fill(new FluidStack(hot,1000),FluidAction.EXECUTE)==0,"Different fuels mixed");
        var saved=be.saveWithoutMetadata(h.getLevel().registryAccess());
        be.tank.drain(4000,FluidAction.EXECUTE);be.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(be.tank.getFluidAmount()==1500,"Save/load changed stored fuel");
        h.runAfterDelay(15,()->{
            h.assertTrue(be.getRemainingBurnTime()>0,"Lava never burned");
            be.tank.drain(4000,FluidAction.EXECUTE);
            h.assertTrue(be.tank.fill(new FluidStack(hot,1000),FluidAction.EXECUTE)==1000,"Empty tank rejected super lava");
        });
        h.runAfterDelay(35,()->{
            h.assertTrue(be.getActiveFuel()==com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity.FuelType.SPECIAL,"Super fuel did not switch heat");
            var state=be.saveWithoutMetadata(h.getLevel().registryAccess());int time=be.getRemainingBurnTime();
            be.loadWithComponents(state,h.getLevel().registryAccess());
            h.assertTrue(be.getRemainingBurnTime()==time,"Reload changed burn time");
            be.tank.drain(4000,FluidAction.EXECUTE);
            be.tank.fill(new FluidStack(Fluids.LAVA,1000),FluidAction.EXECUTE);
            int queued=be.tank.getFluidAmount();
            h.runAfterDelay(5,()->{
                h.assertTrue(be.getActiveFuel()==com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity.FuelType.SPECIAL,"Ordinary fuel downgraded active special fuel");
                h.assertTrue(be.tank.getFluidAmount()==queued,"Queued ordinary fuel consumed before special fuel ended");h.succeed();
            });
        });
    }
}
