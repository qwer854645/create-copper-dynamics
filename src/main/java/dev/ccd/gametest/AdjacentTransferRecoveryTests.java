package dev.ccd.gametest;

import dev.ccd.logistics.AdjacentFluidTransfer;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class AdjacentTransferRecoveryTests {
    @GameTest(template="empty") public static void declinedRollbackRetainsOwnedRemainder(GameTestHelper h){
        var source=new FluidTank(1000){
            @Override public int fill(FluidStack fluid,FluidAction action){return 0;}
        };
        source.setFluid(new FluidStack(Fluids.WATER,1000));
        var target=new FluidTank(1000){
            @Override public int fill(FluidStack fluid,FluidAction action){
                return super.fill(fluid.copyWithAmount(Math.min(fluid.getAmount(),action.simulate()?1000:100)),action);
            }
        };
        var rest=AdjacentFluidTransfer.transfer(source,target,500);
        h.assertTrue(source.getFluidAmount()==500&&target.getFluidAmount()==100&&rest.getAmount()==400,"Unaccepted transfer was lost");
        h.assertTrue(source.getFluidAmount()+target.getFluidAmount()+rest.getAmount()==1000,"Transfer did not conserve fluid");
        var recoverable=new FluidTank(1000);recoverable.setFluid(new FluidStack(Fluids.WATER,1000));
        var another=new FluidTank(1000){@Override public int fill(FluidStack fluid,FluidAction action){return action.simulate()?fluid.getAmount():0;}};
        h.assertTrue(AdjacentFluidTransfer.transfer(recoverable,another,500).isEmpty()&&recoverable.getFluidAmount()==1000,"Return-capable source did not recover fluid");
        h.succeed();
    }
}
