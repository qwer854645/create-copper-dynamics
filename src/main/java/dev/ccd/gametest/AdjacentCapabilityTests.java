package dev.ccd.gametest;

import com.simibubi.create.AllBlocks;
import dev.ccd.CopperDynamics;
import dev.ccd.content.FluidPackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class AdjacentCapabilityTests {
    @GameTest(template="empty") public static void cachedEndpointRefreshesAfterReplacementAndRotation(GameTestHelper h){
        var pos=new BlockPos(2,2,2);
        h.setBlock(pos,CopperDynamics.FLUID_PACKAGER.get().defaultBlockState().setValue(PackagerBlock.FACING,Direction.EAST));
        h.setBlock(pos.west(),AllBlocks.FLUID_TANK.getDefaultState());
        var owner=(FluidPackagerBlockEntity)h.getBlockEntity(pos);
        var first=owner.targetFluidHandler();h.assertTrue(first!=null,"Initial endpoint missing");
        h.setBlock(pos.west(),Blocks.AIR);
        h.assertTrue(owner.targetFluidHandler()==null,"Cached removed tank remained available");
        h.setBlock(pos.west(),AllBlocks.FLUID_TANK.getDefaultState());
        var replacement=owner.targetFluidHandler();
        h.assertTrue(replacement!=null&&replacement!=first,"Replacement tank was not resolved");
        h.setBlock(pos.east(),AllBlocks.FLUID_TANK.getDefaultState());
        h.getLevel().setBlockAndUpdate(h.absolutePos(pos),owner.getBlockState().setValue(PackagerBlock.FACING,Direction.WEST));
        h.assertTrue(owner.targetFluidHandler()!=null&&owner.targetFluidHandler()!=replacement,"Rotated packager kept old endpoint");
        h.succeed();
    }
}
