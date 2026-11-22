package dev.ccd.gametest;
import dev.ccd.CopperDynamics;
import dev.ccd.content.*;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class DirectNativePumpTests {
 @GameTest(template="empty",timeoutTicks=180) public static void omniMultipleInputsAndFullSink(GameTestHelper h){
  var pos=new BlockPos(3,2,3);
  for(var side:java.util.List.of(Direction.WEST,Direction.NORTH,Direction.EAST))h.setBlock(pos.relative(side),AllBlocks.FLUID_TANK.getDefaultState());
  h.setBlock(pos,CopperDynamics.OMNI_PUMP.get().defaultBlockState().setValue(OmniPumpBlock.PORTS.get(Direction.WEST),1).setValue(OmniPumpBlock.PORTS.get(Direction.NORTH),1).setValue(OmniPumpBlock.PORTS.get(Direction.EAST),2));
  var pump=(OmniPumpBlockEntity)h.getBlockEntity(pos);
  h.runAfterDelay(10,()->{
   for(var side:java.util.List.of(Direction.WEST,Direction.NORTH))((FluidTankBlockEntity)h.getBlockEntity(pos.relative(side))).getTankInventory().fill(new FluidStack(Fluids.WATER,8000),FluidAction.EXECUTE);
   pump.setSpeed(64);pump.onSpeedChanged(0);
  });
  h.runAfterDelay(100,()->{
   for(var side:java.util.List.of(Direction.WEST,Direction.NORTH))h.assertTrue(((FluidTankBlockEntity)h.getBlockEntity(pos.relative(side))).getTankInventory().getFluidAmount()<8000,"Input unused: "+side);
   ((FluidTankBlockEntity)h.getBlockEntity(pos.east())).getTankInventory().fill(new FluidStack(Fluids.WATER,8000),FluidAction.EXECUTE);
  });
  h.runAfterDelay(120,()->{
   int before=((FluidTankBlockEntity)h.getBlockEntity(pos.west())).getTankInventory().getFluidAmount()+((FluidTankBlockEntity)h.getBlockEntity(pos.north())).getTankInventory().getFluidAmount();
   h.runAfterDelay(15,()->{int after=((FluidTankBlockEntity)h.getBlockEntity(pos.west())).getTankInventory().getFluidAmount()+((FluidTankBlockEntity)h.getBlockEntity(pos.north())).getTankInventory().getFluidAmount();h.assertTrue(before==after,"Full sink kept draining sources");h.succeed();});
  });
 }
 @GameTest(template="empty",timeoutTicks=200) public static void omniDistributesToMultipleOutputsWithoutStorage(GameTestHelper h){
  var pos=new BlockPos(3,2,3);
  h.setBlock(pos.west(),AllBlocks.FLUID_TANK.getDefaultState());
  h.setBlock(pos.east(),AllBlocks.FLUID_TANK.getDefaultState());
  h.setBlock(pos.south(),AllBlocks.FLUID_TANK.getDefaultState());
  h.setBlock(pos,CopperDynamics.OMNI_PUMP.get().defaultBlockState().setValue(OmniPumpBlock.PORTS.get(Direction.WEST),1).setValue(OmniPumpBlock.PORTS.get(Direction.EAST),2).setValue(OmniPumpBlock.PORTS.get(Direction.SOUTH),2));
  var pump=(OmniPumpBlockEntity)h.getBlockEntity(pos);
  h.runAfterDelay(10,()->{
   ((FluidTankBlockEntity)h.getBlockEntity(pos.west())).getTankInventory().fill(new FluidStack(Fluids.WATER,8000),FluidAction.EXECUTE);
   pump.setSpeed(64);pump.onSpeedChanged(0);
  });
  h.runAfterDelay(120,()->{
   int total=0;
   for(var side:java.util.List.of(Direction.WEST,Direction.EAST,Direction.SOUTH)){
    int amount=((FluidTankBlockEntity)h.getBlockEntity(pos.relative(side))).getTankInventory().getFluidAmount();total+=amount;
    if(side!=Direction.WEST)h.assertTrue(amount>0,"Omni did not supply configured output "+side);
   }
   if(dev.ccd.compat.pnp.PnPCompat.active())total+=dev.ccd.compat.pnp.PnPCompat.stored(h.getLevel(),h.absolutePos(pos));
   h.assertTrue(total==8000,"Multi-output fluid conservation failed: "+total);
   h.assertTrue(h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,h.absolutePos(pos),Direction.WEST)==null,"Omni still exposes owned storage");
   h.succeed();
  });
 }
 @GameTest(template="empty",timeoutTicks=180) public static void directContainersAndBlockedOmni(GameTestHelper h){
  var omni=new BlockPos(2,2,2);var buffer=new BlockPos(2,2,5);var creative=new BlockPos(2,2,8);
  h.setBlock(omni.west(),AllBlocks.FLUID_TANK.getDefaultState());h.setBlock(omni.east(),AllBlocks.FLUID_TANK.getDefaultState());
  h.setBlock(omni,CopperDynamics.OMNI_PUMP.get().defaultBlockState().setValue(OmniPumpBlock.PORTS.get(Direction.WEST),1).setValue(OmniPumpBlock.PORTS.get(Direction.EAST),2));
  h.setBlock(buffer,CopperDynamics.BUFFER_PUMP.get().defaultBlockState().setValue(BufferPumpBlock.FACING,Direction.EAST));h.setBlock(buffer.east(),AllBlocks.FLUID_TANK.getDefaultState());
  h.setBlock(creative,CopperDynamics.CREATIVE_DELIVERY_TANK.get().defaultBlockState().setValue(CreativeDeliveryTankBlock.FACING,Direction.EAST));h.setBlock(creative.east(),AllBlocks.FLUID_TANK.getDefaultState());
  var pump=(OmniPumpBlockEntity)h.getBlockEntity(omni);var stored=(BufferPumpBlockEntity)h.getBlockEntity(buffer);
  h.runAfterDelay(10,()->{
   ((FluidTankBlockEntity)h.getBlockEntity(omni.west())).getTankInventory().fill(new FluidStack(Fluids.WATER,8000),FluidAction.EXECUTE);
   pump.setSpeed(64);pump.onSpeedChanged(0);stored.buffer.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);stored.setSpeed(64);stored.onSpeedChanged(0);
   ((CreativeDeliveryTankBlockEntity)h.getBlockEntity(creative)).select(new FluidStack(Fluids.WATER,1000));
  });
  h.runAfterDelay(100,()->{
   for(var pos:java.util.List.of(omni,buffer,creative))h.assertTrue(((FluidTankBlockEntity)h.getBlockEntity(pos.east())).getTankInventory().getFluidAmount()>0,"Direct native delivery failed: "+pos);
   h.getLevel().setBlockAndUpdate(h.absolutePos(omni),pump.getBlockState().setValue(OmniPumpBlock.PORTS.get(Direction.EAST),0));pump.invalidatePressure();
  });
  h.runAfterDelay(110,()->{
   int before=((FluidTankBlockEntity)h.getBlockEntity(omni.west())).getTankInventory().getFluidAmount();
   h.runAfterDelay(15,()->{h.assertTrue(((FluidTankBlockEntity)h.getBlockEntity(omni.west())).getTankInventory().getFluidAmount()==before,"Omni extracted without output");h.succeed();});
  });
 }
}
