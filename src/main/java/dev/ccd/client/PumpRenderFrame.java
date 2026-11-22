package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.ccd.content.PumpOrientation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Derive the rendering basis from the same signed axes used by pipe/shaft logic. */
public final class PumpRenderFrame {
    private PumpRenderFrame(){}
    public static void apply(PoseStack pose,BlockState state,IntegerProperty roll){
        var right=PumpOrientation.side(state,roll,Direction.EAST);
        var up=PumpOrientation.side(state,roll,Direction.UP);
        var south=state.getValue(com.simibubi.create.content.fluids.pump.PumpBlock.FACING).getOpposite();
        var matrix=new org.joml.Matrix3f(
            right.getStepX(),right.getStepY(),right.getStepZ(),
            up.getStepX(),up.getStepY(),up.getStepZ(),
            south.getStepX(),south.getStepY(),south.getStepZ());
        pose.mulPose(new org.joml.Quaternionf().setFromNormalized(matrix));
    }
}
