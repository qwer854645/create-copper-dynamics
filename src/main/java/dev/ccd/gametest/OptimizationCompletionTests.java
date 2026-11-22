package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.content.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class OptimizationCompletionTests {
    @GameTest(template="empty") public static void pumpMirrorsAndRotationsPreserveFrame(GameTestHelper h){
        for(var block:java.util.List.of(CopperDynamics.REDSTONE_FLUID_PUMP.get(),CopperDynamics.VARIABLE_FLOW_PUMP.get())){
            var roll=block instanceof RedstoneFluidPumpBlock?RedstoneFluidPumpBlock.ROLL:VariableFlowPumpBlock.ROLL;
            for(var front:Direction.values())for(int r=0;r<4;r++){
                var state=block.defaultBlockState().setValue(com.simibubi.create.content.fluids.pump.PumpBlock.FACING,front).setValue(roll,r);
                var right=PumpOrientation.side(state,roll,Direction.EAST);
                var up=PumpOrientation.side(state,roll,Direction.UP);var south=front.getOpposite();
                var basis=new org.joml.Matrix3f(right.getStepX(),right.getStepY(),right.getStepZ(),up.getStepX(),up.getStepY(),up.getStepZ(),south.getStepX(),south.getStepY(),south.getStepZ());
                var quaternion=new org.joml.Quaternionf().setFromNormalized(basis);
                var rendered=new org.joml.Vector3f(1,0,0).rotate(quaternion);
                h.assertTrue(rendered.distance(right.getStepX(),right.getStepY(),right.getStepZ())<.0001f,"Render quaternion disagrees with logical shaft");
                for(var mirror:Mirror.values()){
                    var next=state.mirror(mirror);
                    h.assertTrue(PumpOrientation.side(next,roll,Direction.EAST)==mirror.mirror(PumpOrientation.side(state,roll,Direction.EAST)),"Mirror lost shaft frame");
                    h.assertTrue(next.mirror(mirror)==state,"Double mirror did not restore pose");
                }
                var next=state;
                for(int i=0;i<4;i++)next=next.rotate(Rotation.CLOCKWISE_90);
                h.assertTrue(next==state,"Structure rotation lost roll");
                for(var axis:Direction.Axis.values()){
                    var transform=new com.simibubi.create.content.contraptions.StructureTransform(BlockPos.ZERO,axis,Rotation.CLOCKWISE_90,Mirror.NONE);
                    var transformed=transform.apply(state);
                    h.assertTrue(transformed.getValue(com.simibubi.create.content.fluids.pump.PumpBlock.FACING)==transform.rotateFacing(front),"Contraption lost pipe facing");
                    h.assertTrue(PumpOrientation.side(transformed,roll,Direction.EAST)==transform.rotateFacing(PumpOrientation.side(state,roll,Direction.EAST)),"Contraption lost shaft frame");
                }
            }
        }h.succeed();
    }
}
