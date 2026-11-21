package dev.ccd.content;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Local tank sides and their upper band follow the exact blockstate model rotation. */
public final class CreativeDeliverySlotTransform extends ValueBoxTransform.Sided {
    private static org.joml.Quaternionf orientation(BlockState state){
        var facing=state.getValue(CreativeDeliveryTankBlock.FACING);
        var q=new org.joml.Quaternionf();
        if(facing==Direction.EAST)q.rotateY((float)-Math.PI/2);
        if(facing==Direction.WEST)q.rotateY((float)Math.PI/2);
        float x=switch(facing){case DOWN->180;case NORTH,EAST,WEST->-90;case SOUTH->90;default->0;};
        return q.rotateX((float)Math.toRadians(x));
    }
    private Direction localSide(BlockState state){
        var n=new org.joml.Vector3f(getSide().getStepX(),getSide().getStepY(),getSide().getStepZ());
        orientation(state).conjugate().transform(n);
        return Direction.getNearest(n.x,n.y,n.z);
    }
    // The upper band is a full-width solid lid, not the recessed glass at 15.05/16.
    @Override protected Vec3 getSouthLocation(){return new Vec3(.5,14/16d,1.01);}
    @Override public float getScale(){return .25f;}
    @Override protected boolean isSideActive(BlockState state,Direction side){return side.getAxis()!=state.getValue(CreativeDeliveryTankBlock.FACING).getAxis();}
    @Override public Vec3 getLocalOffset(LevelAccessor level,BlockPos pos,BlockState state){
        var side=localSide(state);if(side.getAxis().isVertical())return null;
        var p=getSouthLocation();
        var v=new org.joml.Vector3f((float)p.x-.5f,(float)p.y-.5f,(float)p.z-.5f);
        new org.joml.Quaternionf().rotateY((float)Math.toRadians(-side.toYRot())).transform(v);
        orientation(state).transform(v);
        return new Vec3(v.x+.5,v.y+.5,v.z+.5);
    }
    @Override public void rotate(LevelAccessor level,BlockPos pos,BlockState state,PoseStack pose){
        pose.mulPose(orientation(state));
        pose.mulPose(Axis.YP.rotationDegrees(180-localSide(state).toYRot()));
    }
}
