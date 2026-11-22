package dev.ccd.content;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import com.simibubi.create.content.fluids.pump.PumpBlock;

/** Preserve the entire local frame when rotating around a clicked world axis. */
public final class PumpOrientation {
    private PumpOrientation(){}
    public static final Direction[] FACINGS={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST,Direction.UP,Direction.DOWN};
    public static BlockState rotate(BlockState state,IntegerProperty roll,Direction clicked){
        return oriented(state,roll,state.getValue(PumpBlock.FACING).getClockWise(clicked.getAxis()),
            side(state,roll,Direction.EAST).getClockWise(clicked.getAxis()));
    }
    public static BlockState oriented(BlockState state,IntegerProperty roll,Direction front,Direction right){
        for(int i=0;i<4;i++){
            var candidate=state.setValue(PumpBlock.FACING,front).setValue(roll,i);
            if(side(candidate,roll,Direction.EAST)==right)return candidate;
        }
        throw new IllegalArgumentException("Pump frame axes must be perpendicular");
    }
    public static BlockState transform(BlockState state,IntegerProperty roll,com.simibubi.create.content.contraptions.StructureTransform transform){
        var front=state.getValue(PumpBlock.FACING);var right=side(state,roll,Direction.EAST);
        if(transform.mirror!=null){front=transform.mirror.mirror(front);right=transform.mirror.mirror(right);}
        return oriented(state,roll,transform.rotateFacing(front),transform.rotateFacing(right));
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
