package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.content.OmniPumpBlock;
import dev.ccd.content.OmniPumpBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraft.world.level.material.Fluids;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class OmniPumpTests {
    @GameTest(template="empty") public static void cyclesOnlyClickedFace(GameTestHelper h){
        var pos=new BlockPos(2,2,2);var block=CopperDynamics.OMNI_PUMP.get();h.setBlock(pos,block.defaultBlockState());
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(AllBlocks.ANDESITE_CASING.get()));
        for(var face:Direction.values()){
            var absolute=h.absolutePos(pos);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(absolute),face,absolute,false);
            var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            for(int expected:new int[]{1,2,3,0}){
                block.onWrenched(h.getBlockState(pos),context);
                h.assertTrue(OmniPumpBlock.mode(h.getBlockState(pos),face)==expected,"Incorrect mode cycle");
                for(var other:Direction.values())if(other!=face)h.assertTrue(OmniPumpBlock.mode(h.getBlockState(pos),other)==0,"Changed another face");
                var pump=(OmniPumpBlockEntity)h.getBlockEntity(pos);
                h.assertTrue((pump.handler(face)!=null)==(expected==1||expected==2),"Wrong fluid capability mode");
                h.assertTrue(block.hasShaftTowards(h.getLevel(),absolute,h.getBlockState(pos),face)==(expected==3),"Wrong shaft face");
            }
        }
        player.getInventory().clearContent();h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=120) public static void configuredPumpTransfersThroughPipes(GameTestHelper h){
        var pos=new BlockPos(3,2,2);var sourcePos=pos.west(2);var sinkPos=pos.east(2);
        h.setBlock(sourcePos,AllBlocks.FLUID_TANK.getDefaultState());h.setBlock(sinkPos,AllBlocks.FLUID_TANK.getDefaultState());
        h.setBlock(pos.west(),AllBlocks.FLUID_PIPE.get().getAxisState(Direction.Axis.X));
        h.setBlock(pos.east(),AllBlocks.FLUID_PIPE.get().getAxisState(Direction.Axis.X));
        h.setBlock(pos,CopperDynamics.OMNI_PUMP.get().defaultBlockState().setValue(OmniPumpBlock.PORTS.get(Direction.WEST),1).setValue(OmniPumpBlock.PORTS.get(Direction.EAST),2).setValue(OmniPumpBlock.PORTS.get(Direction.UP),3));
        var pump=(OmniPumpBlockEntity)h.getBlockEntity(pos);
        h.runAfterDelay(10,()->{
            ((FluidTankBlockEntity)h.getBlockEntity(sourcePos)).getTankInventory().fill(new FluidStack(Fluids.WATER,dev.ccd.compat.pnp.PnPCompat.active()?8000:1000),FluidAction.EXECUTE);
            pump.setSpeed(64);pump.onSpeedChanged(0);
        });
        h.runAfterDelay(60,()->{
            var sink=(FluidTankBlockEntity)h.getBlockEntity(sinkPos);var source=(FluidTankBlockEntity)h.getBlockEntity(sourcePos);
            h.assertTrue(sink.getTankInventory().getFluidAmount()>0,"Pump did not transfer through native pipes; buffer="+pump.buffer.getFluidAmount()+" source="+source.getTankInventory().getFluidAmount());
            int pipeContents=dev.ccd.compat.pnp.PnPCompat.active()?dev.ccd.compat.pnp.PnPCompat.stored(h.getLevel(),h.absolutePos(pos.west()))+dev.ccd.compat.pnp.PnPCompat.stored(h.getLevel(),h.absolutePos(pos.east())):0;
            h.assertTrue(source.getTankInventory().getFluidAmount()+sink.getTankInventory().getFluidAmount()+pump.buffer.getFluidAmount()+pipeContents==(dev.ccd.compat.pnp.PnPCompat.active()?8000:1000),"Fluid was lost or duplicated");
            pump.setSpeed(0);pump.onSpeedChanged(64);int amount=sink.getTankInventory().getFluidAmount();
            int stoppedBuffer=pump.buffer.getFluidAmount();
            h.runAfterDelay(5,()->{h.assertTrue(dev.ccd.compat.pnp.PnPCompat.active()?pump.buffer.getFluidAmount()>=stoppedBuffer:sink.getTankInventory().getFluidAmount()==amount,"Stopped pump still supplies fluid");h.succeed();});
        });
    }
}
