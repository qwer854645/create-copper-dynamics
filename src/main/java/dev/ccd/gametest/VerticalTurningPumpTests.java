package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.content.TurningBoosterPumpBlock;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class VerticalTurningPumpTests {
    @GameTest(template="empty") public static void shaftNormalToBothPorts(GameTestHelper h){
        var block=CopperDynamics.VERTICAL_TURNING_BOOSTER_PUMP.get();
        for(var front:Direction.values())for(int roll=0;roll<4;roll++){
            var state=block.defaultBlockState().setValue(TurningBoosterPumpBlock.FACING,front).setValue(TurningBoosterPumpBlock.ROLL,roll);
            var axis=block.getRotationAxis(state);
            h.assertTrue(axis!=front.getAxis() && axis!=TurningBoosterPumpBlock.branch(state).getAxis(),"Shaft is not normal to ports");
            for(var side:Direction.values()){
                boolean shaft=side.getAxis()==axis;
                h.assertTrue(block.hasShaftTowards(h.getLevel(),h.absolutePos(net.minecraft.core.BlockPos.ZERO),state,side)==shaft,"Incorrect shaft face");
                h.assertTrue(TurningBoosterPumpBlock.shaftModifier(state,side)==(shaft?1:0),"Incorrect straight shaft rotation");
            }
        }
        h.succeed();
    }
}
