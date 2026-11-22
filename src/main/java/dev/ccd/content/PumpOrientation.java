package dev.ccd.content;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import com.simibubi.create.content.fluids.pump.PumpBlock;

/** One accessible wrench face can reach every pipe/shaft orientation. */
public final class PumpOrientation {
    private PumpOrientation(){}
    public static final Direction[] FACINGS={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST,Direction.UP,Direction.DOWN};
    public static BlockState next(BlockState state,IntegerProperty roll){
        int current=0;
        for(int i=0;i<FACINGS.length;i++)if(FACINGS[i]==state.getValue(PumpBlock.FACING))current=i;
        int next=(current+1)%FACINGS.length;
        return state.setValue(PumpBlock.FACING,FACINGS[next]).setValue(roll,(state.getValue(roll)+(next==0?1:0))%4);
    }
    public static Direction side(BlockState state,IntegerProperty roll,Direction side){
        for(int i=0;i<state.getValue(roll);i++)side=side.getClockWise(Direction.Axis.Z);
        var front=state.getValue(PumpBlock.FACING);
        if(front.getAxis().isVertical())return front==Direction.UP?side.getCounterClockWise(Direction.Axis.X):side.getClockWise(Direction.Axis.X);
        int turns=switch(front){case EAST->1;case SOUTH->2;case WEST->3;default->0;};
        for(int i=0;i<turns;i++)side=side.getClockWise(Direction.Axis.Y);
        return side;
    }
}
