package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class PumpWrenchCycleTests {
    @GameTest(template="empty") public static void everySingleFaceReachesAllOrientations(GameTestHelper h){
        for(var block:java.util.List.of(CopperDynamics.REDSTONE_FLUID_PUMP.get(),CopperDynamics.VARIABLE_FLOW_PUMP.get())){
            var wrench=(com.simibubi.create.content.equipment.wrench.IWrenchable)block;
            for(var clicked:Direction.values()){
                var initial=block.defaultBlockState();var state=initial;
                var reached=new java.util.HashSet<net.minecraft.world.level.block.state.BlockState>();
                var pos=new net.minecraft.core.BlockPos(2,2,2);
                h.setBlock(pos,initial);
                var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,com.simibubi.create.AllItems.WRENCH.asStack());
                var absolute=h.absolutePos(pos);
                var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(absolute),clicked,absolute,false));
                for(int i=0;i<24;i++){
                    state=h.getBlockState(pos);h.assertTrue(reached.add(state),"Repeated orientation too early");
                    wrench.onWrenched(state,context);
                }
                state=h.getBlockState(pos);
                h.assertTrue(reached.size()==24&&state==initial,"Single face cannot reach all 24 poses: "+clicked);
            }
        }
        h.succeed();
    }
}
