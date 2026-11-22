package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.content.*;
import dev.ccd.logistics.AdjacentFluidTransfer;
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
    @GameTest(template="empty") public static void realOmniRollbackPersistsAndDropsOverflow(GameTestHelper h){
        var pos=new BlockPos(2,2,2);
        h.setBlock(pos,CopperDynamics.OMNI_PUMP.get().defaultBlockState().setValue(OmniPumpBlock.PORTS.get(Direction.EAST),OmniPumpBlock.OUTPUT));
        var pump=(OmniPumpBlockEntity)h.getBlockEntity(pos);pump.setSpeed(1024);
        pump.buffer.setFluid(new FluidStack(Fluids.WATER,1000));
        var rejecting=new FluidTank(1000){@Override public int fill(FluidStack fluid,FluidAction action){return action.simulate()?fluid.getAmount():0;}};
        var rest=AdjacentFluidTransfer.transfer(pump.handler(Direction.EAST),rejecting,500);
        h.assertTrue(rest.getAmount()==500,"Output wrapper swallowed rollback");
        AdjacentFluidTransfer.recover(pump,rest);
        h.assertTrue(pump.buffer.getFluidAmount()==1000,"Real buffer did not receive rollback");
        var saved=pump.saveWithoutMetadata(h.getLevel().registryAccess());pump.buffer.setFluid(FluidStack.EMPTY);pump.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(pump.buffer.getFluidAmount()==1000,"Rollback lost on reload");
        pump.buffer.setFluid(new FluidStack(Fluids.WATER,2000));
        // Other tests reload resources later; keep this recovery scenario isolated in time.
        AdjacentFluidTransfer.recover(pump,new FluidStack(Fluids.WATER,500));
        h.runAfterDelay(5,()->{
            var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(h.absolutePos(pos)).inflate(3));
            var parcels=h.getLevel().getEntitiesOfClass(com.simibubi.create.content.logistics.box.PackageEntity.class,new net.minecraft.world.phys.AABB(h.absolutePos(pos)).inflate(3));
            int amount=drops.stream().mapToInt(e->FluidPackageItem.contents(e.getItem()).getAmount()).sum()+parcels.stream().mapToInt(e->FluidPackageItem.contents(e.box).getAmount()).sum();
            h.assertTrue(amount==500,"Overflow parcel lost payload: "+amount);
            h.setBlock(pos,Blocks.AIR);
            h.assertTrue(drops.stream().mapToInt(e->FluidPackageItem.contents(e.getItem()).getAmount()).sum()+parcels.stream().mapToInt(e->FluidPackageItem.contents(e.box).getAmount()).sum()==500,"Removing machine changed recovery parcel");h.succeed();
        });
    }
}
