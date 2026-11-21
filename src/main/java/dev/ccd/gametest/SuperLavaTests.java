package dev.ccd.gametest;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import dev.ccd.compat.ProcessingMedia;
import dev.ccd.compat.SuperLavaFuel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class SuperLavaTests {
    @GameTest(template="empty") public static void superLavaFuelsNativeBurner(GameTestHelper h){
        var medium=ProcessingMedia.ALL.getFirst();h.assertTrue(medium.available(),"Super lava hidden without optional mod");
        var pos=new BlockPos(2,2,2);h.setBlock(pos,AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL,BlazeBurnerBlock.HeatLevel.SMOULDERING));
        var burner=(BlazeBurnerBlockEntity)h.getBlockEntity(pos);var handler=new SuperLavaFuel(burner);
        var lava=new FluidStack(medium.source.get(),1000);
        h.assertTrue(handler.fill(lava,FluidAction.SIMULATE)==1000&&burner.getRemainingBurnTime()==0,"Simulation changed fuel");
        h.assertTrue(handler.fill(lava.copyWithAmount(400),FluidAction.EXECUTE)==400,"Partial pipe input rejected");
        handler=new SuperLavaFuel(burner);
        h.assertTrue(handler.getFluidInTank(0).getAmount()==400,"Partial fuel not retained");
        h.assertTrue(handler.fill(lava,FluidAction.EXECUTE)==600,"Incorrect full bucket consumption");
        h.assertTrue(burner.getActiveFuel()==BlazeBurnerBlockEntity.FuelType.SPECIAL&&burner.getRemainingBurnTime()==3200,"Superheat fuel failed");
        h.assertTrue(handler.fill(lava,FluidAction.EXECUTE)==0,"Overfilled active burner");h.succeed();
    }
}
