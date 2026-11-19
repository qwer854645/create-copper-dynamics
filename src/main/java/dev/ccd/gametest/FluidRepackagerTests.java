package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.logistics.CompositeRepackageHelper;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.packager.repackager.RepackagerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd")
@PrefixGameTestTemplate(false)
public class FluidRepackagerTests {
    @GameTest(template="empty")
    public static void onlyDedicatedMachineUsesFluidHelper(GameTestHelper h) {
        var nativePos=new BlockPos(1,1,1);var fluidPos=new BlockPos(3,1,1);
        h.setBlock(nativePos,AllBlocks.REPACKAGER.get());
        h.setBlock(fluidPos,CopperDynamics.FLUID_REPACKAGER.get());
        var original=(RepackagerBlockEntity)h.getBlockEntity(nativePos);
        var fluid=(RepackagerBlockEntity)h.getBlockEntity(fluidPos);
        h.assertTrue(!(original.repackageHelper instanceof CompositeRepackageHelper),"Native repackager was modified");
        h.assertTrue(fluid.repackageHelper instanceof CompositeRepackageHelper,"Dedicated helper missing");
        h.assertTrue(h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
            h.absolutePos(fluidPos),null)!=null,"Dedicated inventory capability missing");
        h.succeed();
    }
}
