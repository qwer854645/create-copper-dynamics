package dev.ccd.gametest;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import dev.ccd.logistics.FluidArmPoint;
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
public class ArmBurnerTests {
    @GameTest(template="empty") public static void directBurnerAcceptsLavaWithoutHatch(GameTestHelper h){
        var pos=new BlockPos(2,2,2);
        h.setBlock(pos,AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL,BlazeBurnerBlock.HeatLevel.SMOULDERING));
        var absolute=h.absolutePos(pos);
        h.assertTrue(FluidArmPoint.canTarget(h.getLevel(),absolute),"Burner requires a hatch");
        var handler=FluidArmPoint.interactionHandler(h.getLevel(),absolute,Direction.UP);
        var burner=(BlazeBurnerBlockEntity)h.getBlockEntity(pos);
        h.assertTrue(handler.fill(new FluidStack(Fluids.LAVA,1000),FluidAction.SIMULATE)==1000,"Lava simulation rejected");
        h.assertTrue(burner.getRemainingBurnTime()==0,"Simulation consumed fuel");
        h.assertTrue(handler.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE)==0,"Water accepted as fuel");
        h.assertTrue(handler.fill(new FluidStack(Fluids.LAVA,500),FluidAction.EXECUTE)==500,"Partial lava rejected");
        h.assertTrue(handler.fill(new FluidStack(Fluids.LAVA,500),FluidAction.EXECUTE)==500,"Remaining lava rejected");
        h.assertTrue(burner.getActiveFuel()==BlazeBurnerBlockEntity.FuelType.NORMAL&&burner.getRemainingBurnTime()>0,"Lava failed to fuel burner");h.succeed();
    }
}
