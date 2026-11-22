package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class PumpWrenchCycleTests {
    @GameTest(template="empty") public static void wrenchRotatesWholeFrameAroundClickedAxis(GameTestHelper h){
        for(var block:java.util.List.of(CopperDynamics.REDSTONE_FLUID_PUMP.get(),CopperDynamics.VARIABLE_FLOW_PUMP.get())){
            var wrench=(com.simibubi.create.content.equipment.wrench.IWrenchable)block;
            var roll=block instanceof dev.ccd.content.RedstoneFluidPumpBlock?dev.ccd.content.RedstoneFluidPumpBlock.ROLL:dev.ccd.content.VariableFlowPumpBlock.ROLL;
            for(var facing:Direction.values())for(int r=0;r<4;r++)for(var clicked:Direction.values()){
                var initial=block.defaultBlockState().setValue(com.simibubi.create.content.fluids.pump.PumpBlock.FACING,facing).setValue(roll,r);var state=initial;
                var reached=new java.util.HashSet<net.minecraft.world.level.block.state.BlockState>();
                var pos=new net.minecraft.core.BlockPos(2,2,2);
                h.setBlock(pos,initial);
                var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,com.simibubi.create.AllItems.WRENCH.asStack());
                var absolute=h.absolutePos(pos);
                var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(absolute),clicked,absolute,false));
                for(int i=0;i<4;i++){
                    state=h.getBlockState(pos);h.assertTrue(reached.add(state),"Repeated orientation too early");
                    var expectedFront=state.getValue(com.simibubi.create.content.fluids.pump.PumpBlock.FACING).getClockWise(clicked.getAxis());
                    var expectedRight=dev.ccd.content.PumpOrientation.side(state,roll,Direction.EAST).getClockWise(clicked.getAxis());
                    wrench.onWrenched(state,context);
                    var actual=h.getBlockState(pos);
                    h.assertTrue(actual.getValue(com.simibubi.create.content.fluids.pump.PumpBlock.FACING)==expectedFront,"Pipe axis did not follow clicked axis");
                    h.assertTrue(dev.ccd.content.PumpOrientation.side(actual,roll,Direction.EAST)==expectedRight,"Shaft frame was not rotated with body");
                }
                state=h.getBlockState(pos);
                h.assertTrue(reached.size()==4&&state==initial,"Four quarter turns did not restore original pose: "+clicked);
            }
        }
        h.succeed();
    }
}
