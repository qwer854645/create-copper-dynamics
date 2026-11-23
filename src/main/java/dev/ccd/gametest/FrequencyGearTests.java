package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.content.VariableFlowPumpBlock;
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
    @GameTest(template="empty") public static void pipeFacesRejectAllCogHelpers(GameTestHelper h) throws Exception {
        var pos=new BlockPos(3,3,3);var absolute=h.absolutePos(pos);
        for(String helperName:java.util.List.of("SmallCogHelper","LargeCogHelper","IntegratedSmallCogHelper")){
            var type=Class.forName("com.simibubi.create.content.kinetics.simpleRelays.CogwheelBlockItem$"+helperName);
            var constructor=type.getDeclaredConstructor();constructor.setAccessible(true);
            var helper=(net.createmod.catnip.placement.IPlacementHelper)constructor.newInstance();
            for(var facing:Direction.values())for(int roll=0;roll<4;roll++){
                var state=CopperDynamics.VARIABLE_FLOW_PUMP.get().defaultBlockState().setValue(VariableFlowPumpBlock.FACING,facing).setValue(VariableFlowPumpBlock.ROLL,roll);
                h.setBlock(pos,state);
                for(var side:java.util.List.of(facing,facing.getOpposite())){
                    var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(absolute)
                        .add(net.minecraft.world.phys.Vec3.atLowerCornerOf(side.getNormal()).scale(.5)),side,absolute,false);
                    h.assertTrue(!helper.getOffset(null,h.getLevel(),state,absolute,hit).isSuccessful(),"Pipe face still previews cog: "+helperName+" "+facing+" roll="+roll);
                }
            }
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void pipeFacesCannotMesh(GameTestHelper h){
        var pos=new BlockPos(3,3,3);
        for(var facing:Direction.values())for(int roll=0;roll<4;roll++){
            h.setBlock(pos,CopperDynamics.VARIABLE_FLOW_PUMP.get().defaultBlockState().setValue(VariableFlowPumpBlock.FACING,facing).setValue(VariableFlowPumpBlock.ROLL,roll));
            var pump=(KineticBlockEntity)h.getBlockEntity(pos);
            var axis= CopperDynamics.VARIABLE_FLOW_PUMP.get().getRotationAxis(pump.getBlockState());
            for(var side:Direction.values()){
                var neighbor=pos.relative(side);
                h.setBlock(neighbor,AllBlocks.COGWHEEL.getDefaultState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS,axis));
                var gear=(KineticBlockEntity)h.getBlockEntity(neighbor);
                var expectedAxis=roll%2==0?(facing.getAxis().isVertical()?Direction.Axis.Z:Direction.Axis.Y)
                    :(facing.getAxis()==Direction.Axis.X?Direction.Axis.Z:Direction.Axis.X);
                boolean expected=side.getAxis()==expectedAxis;
                h.assertTrue(RotationPropagator.isConnected(pump,gear)==expected,"Incorrect pump gear connection at "+facing+" / "+side);
                h.assertTrue(RotationPropagator.isConnected(gear,pump)==expected,"Incorrect reverse gear connection");
                h.setBlock(neighbor,net.minecraft.world.level.block.Blocks.AIR);
            }
            h.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR);
        }
        h.succeed();
    }
}
