package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd")
@PrefixGameTestTemplate(false)
public class CollisionShapeTests {
    @GameTest(template="empty")
    public static void coarseShapesMatchSelection(GameTestHelper h) {
        for(var block:java.util.List.of(CopperDynamics.COPPER_WATER_BASIN.get(),CopperDynamics.COPPER_COLLECTION_GRATE.get(),
            CopperDynamics.SMART_COLLECTION_GRATE.get(),CopperDynamics.FAUCET.get(),CopperDynamics.SMART_FAUCET.get(),
            CopperDynamics.REDSTONE_FLUID_VALVE.get())) {
            int max=0;
            for(var state:block.getStateDefinition().getPossibleStates()) {
                var pos=h.absolutePos(BlockPos.ZERO);
                var shape=state.getShape(h.getLevel(),pos);
                var collision=state.getCollisionShape(h.getLevel(),pos,CollisionContext.empty());
                var interaction=state.getInteractionShape(h.getLevel(),pos);
                h.assertTrue(!Shapes.joinIsNotEmpty(shape,collision,BooleanOp.NOT_SAME),"Collision/selection differ: "+block);
                h.assertTrue(!Shapes.joinIsNotEmpty(shape,interaction,BooleanOp.NOT_SAME),"Interaction/selection differ: "+block);
                max=Math.max(max,collision.toAabbs().size());
            }
            System.out.println("CCD_OPTIMIZED_SHAPE "+BuiltInRegistries.BLOCK.getKey(block)+" boxes="+max);
            h.assertTrue(max<=8,"Coarse collision unexpectedly fragmented: "+block);
        }
        h.succeed();
    }
}
