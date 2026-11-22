package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.content.TankBlazeBurnerBlockEntity;
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
public class TankBurnerTests {
    @GameTest(template="empty",timeoutTicks=80) public static void tankFeedsNativeBurnerAndArmUsesTank(GameTestHelper h){
        var pos=new BlockPos(2,2,2);h.setBlock(pos,CopperDynamics.TANK_BURNER.get().defaultBlockState());
        var be=(TankBlazeBurnerBlockEntity)h.getBlockEntity(pos);
        var handler=dev.ccd.logistics.FluidArmPoint.interactionHandler(h.getLevel(),h.absolutePos(pos),Direction.UP);
        h.assertTrue(dev.ccd.logistics.FluidArmPoint.canTarget(h.getLevel(),h.absolutePos(pos))&&handler==be.tank,"Arm did not target actual tank");
        h.assertTrue(handler.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE)==0,"Nonfuel accepted");
        h.assertTrue(handler.fill(new FluidStack(Fluids.LAVA,4000),FluidAction.SIMULATE)==4000&&be.tank.isEmpty(),"Simulation mutated storage");
        handler.fill(new FluidStack(Fluids.LAVA,4000),FluidAction.EXECUTE);
        var tag=be.saveWithoutMetadata(h.getLevel().registryAccess());be.tank.setFluid(FluidStack.EMPTY);be.loadWithComponents(tag,h.getLevel().registryAccess());
        h.assertTrue(be.tank.getFluidAmount()==4000,"Tank did not persist");
        h.runAfterDelay(15,()->{
            h.assertTrue(be.getRemainingBurnTime()>0,"Tank did not feed native burner");
            var fuel=dev.ccd.compat.LiquidFuelCompat.find(Fluids.LAVA);
            int quantum=fuel==null?1000:fuel.amount();
            h.assertTrue(be.tank.getFluidAmount()<4000&&(4000-be.tank.getFluidAmount())%quantum==0,"Fuel volume not consumed at configured rate");
            h.assertTrue(com.simibubi.create.content.processing.burner.BlazeBurnerBlock.getHeatLevelOf(be.getBlockState()).isAtLeast(com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.KINDLED),"Heat not available to recipes");h.succeed();
        });
    }
}
