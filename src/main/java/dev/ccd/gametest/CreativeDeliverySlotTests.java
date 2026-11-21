package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.content.CreativeDeliveryTankBlock;
import dev.ccd.content.CreativeDeliverySlotTransform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class CreativeDeliverySlotTests {
    @GameTest(template="empty") public static void upperSlotsStayOutsideRotatedLid(GameTestHelper h){
        for(var front:Direction.values()){
            var state=CopperDynamics.CREATIVE_DELIVERY_TANK.get().defaultBlockState().setValue(CreativeDeliveryTankBlock.FACING,front);
            int count=0;
            for(var side:Direction.values()){
                var slot=new CreativeDeliverySlotTransform();slot.fromSide(side);
                if(side.getAxis()==front.getAxis()){
                    h.assertTrue(!slot.shouldRender(h.getLevel(),BlockPos.ZERO,state),"Slot shown on tank end");continue;
                }
                count++;
                var offset=slot.getLocalOffset(h.getLevel(),BlockPos.ZERO,state).subtract(.5,.5,.5);
                var normal=Vec3.atLowerCornerOf(side.getNormal());
                var top=Vec3.atLowerCornerOf(front.getNormal());
                h.assertTrue(Math.abs(offset.dot(normal)-.51)<.0001,"Slot buried inside lid: "+front+" / "+side);
                h.assertTrue(Math.abs(offset.dot(top)-.375)<.0001,"Slot does not follow model top");
                h.assertTrue(slot.testHit(h.getLevel(),BlockPos.ZERO,state,offset.add(.5,.5,.5)),"Slot center is not clickable");
            }
            h.assertTrue(count==4,"Expected four side slots");
        }
        h.succeed();
    }
}
