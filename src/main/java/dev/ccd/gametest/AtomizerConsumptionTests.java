package dev.ccd.gametest;

import dev.ccd.*;
import dev.ccd.content.FluidAtomizerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class AtomizerConsumptionTests {
    @GameTest(template="empty") public static void zeroCostKeepsMediumAndStillNeedsFluid(GameTestHelper h) throws Exception {
        var setting=CCDConfig.ATOMIZER_CONSUMPTION_MULTIPLIER;
        double previous=setting.get();
        var pos=new BlockPos(2,2,2);h.setBlock(pos,CopperDynamics.FLUID_ATOMIZER.get());
        var atomizer=(FluidAtomizerBlockEntity)h.getBlockEntity(pos);
        var method=FluidAtomizerBlockEntity.class.getDeclaredMethod("prepareMist");method.setAccessible(true);
        try {
            setting.set(0d);setting.clearCache();atomizer.setSpeed(32);
            method.invoke(atomizer);
            h.assertTrue(!atomizer.mistActive(),"Empty atomizer produced free effects");
            var medium=dev.ccd.compat.ProcessingMedia.ALL.stream().filter(m->m.id.equals("smoking_fluid")).findFirst().orElseThrow();
            var handler=atomizer.fluidHandler(null);
            handler.fill(new net.neoforged.neoforge.fluids.FluidStack(medium.source.get(),1000),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            method.invoke(atomizer);
            h.assertTrue(atomizer.mistActive()&&atomizer.activeFluid().getFluid()==medium.source.get(),"Zero cost lost processing medium");
            h.assertTrue(handler.getFluidInTank(0).getAmount()==1000,"Zero cost consumed fluid");
            handler.drain(1000,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            method.invoke(atomizer);
            h.assertTrue(!atomizer.mistActive(),"Zero cost continued after removing medium");
            setting.set(1d);setting.clearCache();
            h.assertTrue(FluidAtomizerBlockEntity.consumptionAmount(10)==10,"Unit multiplier changed base cost");
            setting.set(.25d);setting.clearCache();
            h.assertTrue(FluidAtomizerBlockEntity.consumptionAmount(10)==3,"Fractional cost did not round up");
        } finally {setting.set(previous);setting.clearCache();}
        h.assertTrue(setting.getDefault()==0d,"Consumption must default to zero");h.succeed();
    }
}
