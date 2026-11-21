package dev.ccd.gametest;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import dev.ccd.CopperDynamics;
import dev.ccd.content.BufferPumpBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class BufferPumpLifecycleTests {
    @GameTest(template="empty") public static void storagePumpHasNoTransparentTransport(GameTestHelper h){
        var pos=new BlockPos(2,2,2);
        h.setBlock(pos,CopperDynamics.BUFFER_PUMP.get().defaultBlockState());
        h.runAfterDelay(3,()->{
            var pump=(BufferPumpBlockEntity)h.getBlockEntity(pos);
            h.assertTrue(pump.getBehaviour(FluidTransportBehaviour.TYPE)==null,"Buffer pump retained crashing native transport");
            for(var side:Direction.values())pump.updatePipesOnSide(side);
            pump.updatePressureChange();
            var fluid=new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000);
            pump.buffer.fill(fluid,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            var saved=pump.saveWithFullMetadata(h.getLevel().registryAccess());
            var restored=new BufferPumpBlockEntity(pump.getBlockPos(),pump.getBlockState());
            restored.setLevel(h.getLevel());restored.loadWithComponents(saved,h.getLevel().registryAccess());
            restored.initialize();restored.tick();
            h.assertTrue(restored.buffer.getFluidAmount()==1000,"Storage lost during transport lifecycle fix");
            h.succeed();
        });
    }
}
