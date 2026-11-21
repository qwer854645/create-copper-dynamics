package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class WrenchRotationTests {
    @GameTest(template="empty") public static void directionalComponentsRotateFromEveryFace(GameTestHelper h){
        for(var block:java.util.List.of(CopperDynamics.RECIPROCATING_PUMP.get(),CopperDynamics.PORTABLE_PUMP.get(),CopperDynamics.FLUID_ATOMIZER.get(),CopperDynamics.FLUID_HATCH.get(),CopperDynamics.SMART_FLUID_HATCH.get(),CopperDynamics.FLUID_ACCESS_PORT.get(),CopperDynamics.FLUID_BRIDGE.get(),CopperDynamics.CREATIVE_DELIVERY_TANK.get())){
            var wrenchable=(IWrenchable)block;
            for(var state:block.getStateDefinition().getPossibleStates()){
                // The frequency and portable models only support horizontal placement.
                if(block instanceof dev.ccd.content.ReciprocatingFluidPumpBlock&&state.getValue(com.simibubi.create.content.fluids.pump.PumpBlock.FACING).getAxis().isVertical())continue;
                for(var face:Direction.values())h.assertTrue(wrenchable.getRotatedBlockState(state,face)!=state,"No rotation for "+block+" from "+face);
            }
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void creativeTankHasOnlyUpperModeControl(GameTestHelper h){
        var pos=new BlockPos(2,2,2);h.setBlock(pos,CopperDynamics.CREATIVE_DELIVERY_TANK.get());
        var be=(dev.ccd.content.CreativeDeliveryTankBlockEntity)h.getBlockEntity(pos);
        h.assertTrue(be.getBehaviour(com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour.TYPE)==null,"Unused filter slot remains");
        h.assertTrue(be.worldOutput.getSlotPositioning() instanceof dev.ccd.content.CreativeDeliverySlotTransform,"Mode control not on rotating upper band");h.succeed();
    }
}
