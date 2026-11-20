package dev.ccd.content;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class ReciprocatingPumpDriveBlockEntity extends KineticBlockEntity {
    public ReciprocatingPumpDriveBlockEntity(BlockPos pos,BlockState state){super(CopperDynamics.RECIPROCATING_DRIVE_BE.get(),pos,state);}
    @Override public void onSpeedChanged(float old){
        super.onSpeedChanged(old);
        if(level!=null&&!level.isClientSide()&&level.getBlockEntity(worldPosition.below()) instanceof ReciprocatingFluidPumpBlockEntity pump)pump.updatePressureChange();
    }
}
