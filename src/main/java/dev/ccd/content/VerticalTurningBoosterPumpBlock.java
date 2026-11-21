package dev.ccd.content;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Straight shaft perpendicular to the plane of the two fluid ports. */
public final class VerticalTurningBoosterPumpBlock extends TurningBoosterPumpBlock {
    public VerticalTurningBoosterPumpBlock(Properties properties) { super(properties); }
    @Override public Direction.Axis getRotationAxis(BlockState state) {
        var front=state.getValue(FACING).getAxis();
        var branch=branch(state).getAxis();
        for(var axis:Direction.Axis.values())if(axis!=front && axis!=branch)return axis;
        throw new IllegalStateException("Turning pump ports must be perpendicular");
    }
}
