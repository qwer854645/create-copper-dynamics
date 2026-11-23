package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.content.PortableFluidPumpBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class PortablePumpTests {
    @GameTest(template="empty") public static void allPortablePosesRotateShapeAndShaft(GameTestHelper h){
        var block=CopperDynamics.PORTABLE_PUMP.get();var pos=new BlockPos(2,2,2);
        for(var front:Direction.values())for(int roll=0;roll<4;roll++){
            var state=block.defaultBlockState().setValue(dev.ccd.content.PortablePumpBaseBlock.FACING,front).setValue(dev.ccd.content.PortablePumpBaseBlock.ROLL,roll);
            h.setBlock(pos,state);var pump=(PortableFluidPumpBlockEntity)h.getBlockEntity(pos);
            pump.stroke=0;var expanded=block.getShape(state,h.getLevel(),h.absolutePos(pos),net.minecraft.world.phys.shapes.CollisionContext.empty()).bounds();
            var collision=block.getCollisionShape(state,h.getLevel(),h.absolutePos(pos),net.minecraft.world.phys.shapes.CollisionContext.empty());
            pump.stroke=1;var compressed=block.getShape(state,h.getLevel(),h.absolutePos(pos),net.minecraft.world.phys.shapes.CollisionContext.empty()).bounds();
            h.assertTrue(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(collision,block.getCollisionShape(state,h.getLevel(),h.absolutePos(pos),net.minecraft.world.phys.shapes.CollisionContext.empty()),net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME),"Collision changed with bellows stroke");
            h.assertTrue(collision.bounds().equals(expanded),"Stable collision differs from expanded profile");
            var strokeAxis=dev.ccd.content.PumpOrientation.side(state,dev.ccd.content.PortablePumpBaseBlock.ROLL,Direction.UP).getAxis();
            double a=strokeAxis.choose(expanded.getXsize(),expanded.getYsize(),expanded.getZsize());
            double b=strokeAxis.choose(compressed.getXsize(),compressed.getYsize(),compressed.getZsize());
            h.assertTrue(a>b,"Rotated bellows shape did not compress: "+front+"/"+roll);
            int shafts=0;for(var side:Direction.values())if(block.hasShaftTowards(h.getLevel(),h.absolutePos(pos),state,side)){shafts++;h.assertTrue(side.getAxis()!=front.getAxis(),"Shaft overlaps mouth");}
            h.assertTrue(shafts==2,"Wrong shaft count");
        }h.succeed();
    }
    @GameTest(template="empty") public static void handDrivePowersShaftAndStops(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);
        h.setBlock(pos,CopperDynamics.PORTABLE_PUMP.get().defaultBlockState());
        h.setBlock(pos.east(),com.simibubi.create.AllBlocks.SHAFT.getDefaultState()
            .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS,Direction.Axis.X));
        h.runAfterDelay(3,()->{
            var pump=(PortableFluidPumpBlockEntity)h.getBlockEntity(pos);
            pump.turn(false);
            h.runAfterDelay(2,()->{
                var shaft=(com.simibubi.create.content.kinetics.base.KineticBlockEntity)h.getBlockEntity(pos.east());
                h.assertTrue(shaft.getSpeed()==32,"Hand drive did not power adjacent shaft");
                h.assertTrue(pump.calculateAddedStressCapacity()>pump.calculateStressApplied(),"No spare capacity for external devices");
                var state=pump.getBlockState();
                var flipped=CopperDynamics.PORTABLE_PUMP.get().getRotatedBlockState(state,Direction.UP);
                 h.assertTrue(flipped.getValue(dev.ccd.content.PortableFluidPumpBlock.FACING)==state.getValue(dev.ccd.content.PortableFluidPumpBlock.FACING).getClockWise(Direction.Axis.Y),"Wrench did not rotate body");
                h.assertTrue(!pump.isPullingOnSide(true),"Positive speed must push at front");
                pump.turn(true);
                h.assertTrue(pump.getGeneratedSpeed()==-32&&pump.isPullingOnSide(true),"Sneaking must reverse shaft and flow");
                h.runAfterDelay(12,()->{
                    h.assertTrue(shaft.getSpeed()==0,"Manual source did not stop");h.succeed();
                });
            });
        });
    }
}
