package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class BufferGearTests {
    @GameTest(template="empty") public static void bufferMeshesWithNativeCogwheels(GameTestHelper h){
        var pos=new BlockPos(3,3,3);
        for(var front:Direction.values()){
            var state=CopperDynamics.BUFFER_PUMP.get().defaultBlockState().setValue(com.simibubi.create.content.fluids.pump.PumpBlock.FACING,front);
            h.setBlock(pos,state);
            var pump=(com.simibubi.create.content.kinetics.base.KineticBlockEntity)h.getBlockEntity(pos);
            for(var side:Direction.values())if(side.getAxis()!=front.getAxis()){
                var target=pos.relative(side);
                h.setBlock(target,com.simibubi.create.AllBlocks.COGWHEEL.getDefaultState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS,front.getAxis()));
                var gear=(com.simibubi.create.content.kinetics.base.KineticBlockEntity)h.getBlockEntity(target);
                h.assertTrue(com.simibubi.create.content.kinetics.RotationPropagator.isConnected(pump,gear),"Buffer gear disconnected: "+front+" / "+side);
                h.assertTrue(com.simibubi.create.content.kinetics.RotationPropagator.isConnected(gear,pump),"Reverse gear connection missing");
                var helper=new com.simibubi.create.content.kinetics.simpleRelays.CogwheelBlockItem.IntegratedSmallCogHelper();
                h.setBlock(target,net.minecraft.world.level.block.Blocks.AIR);
                var absolute=h.absolutePos(pos);
                var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(absolute),side,absolute,false);
                var offset=helper.getOffset(h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE),h.getLevel(),state,absolute,hit);
                h.assertTrue(offset.isSuccessful(),"Buffer gear placement failed");
            }
            h.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR);
        }
        h.succeed();
    }
}
