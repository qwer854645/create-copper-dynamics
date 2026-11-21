package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.content.ReciprocatingFluidPumpBlock;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class FrequencyGearTests {
    @GameTest(template="empty") public static void pipeFacesCannotMesh(GameTestHelper h){
        var pos=new BlockPos(3,3,3);
        for(var facing:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST}){
            h.setBlock(pos,CopperDynamics.RECIPROCATING_PUMP.get().defaultBlockState().setValue(ReciprocatingFluidPumpBlock.FACING,facing));
            var pump=(KineticBlockEntity)h.getBlockEntity(pos);
            var axis= CopperDynamics.RECIPROCATING_PUMP.get().getRotationAxis(pump.getBlockState());
            for(var side:Direction.values()){
                var neighbor=pos.relative(side);
                h.setBlock(neighbor,AllBlocks.COGWHEEL.getDefaultState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS,axis));
                var gear=(KineticBlockEntity)h.getBlockEntity(neighbor);
                boolean expected=side.getAxis()==Direction.Axis.Y;
                h.assertTrue(RotationPropagator.isConnected(pump,gear)==expected,"Incorrect pump gear connection at "+facing+" / "+side);
                h.assertTrue(RotationPropagator.isConnected(gear,pump)==expected,"Incorrect reverse gear connection");
                h.setBlock(neighbor,net.minecraft.world.level.block.Blocks.AIR);
            }
            h.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR);
        }
        h.succeed();
    }
}
