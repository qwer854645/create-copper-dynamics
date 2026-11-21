package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.ccd.content.ThreeWayValveBlock;
import dev.ccd.content.ThreeWayValveBlockEntity;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public final class ThreeWayValveRenderer extends SmartBlockEntityRenderer<ThreeWayValveBlockEntity> {
    private static final PartialModel POINTER=PartialModel.of(ResourceLocation.fromNamespaceAndPath("ccd","block/three_way_valve/pointer"));
    private static final PartialModel DIAL=PartialModel.of(ResourceLocation.fromNamespaceAndPath("ccd","block/three_way_valve/dial_unpowered"));
    private static final PartialModel DIAL_MIRRORED=PartialModel.of(ResourceLocation.fromNamespaceAndPath("ccd","block/three_way_valve/dial_mirrored"));
    public static void registerModels() {}
    public ThreeWayValveRenderer(BlockEntityRendererProvider.Context context) { super(context); }
    @Override protected void renderSafe(ThreeWayValveBlockEntity be,float pt,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        super.renderSafe(be,pt,pose,buffers,light,overlay);
        var state=be.getBlockState();
        var axis=state.getValue(ThreeWayValveBlock.FACING).getAxis();
        var branch=ThreeWayValveBlock.branch(state).getAxis();
        Direction normal=axis!=Direction.Axis.X && branch!=Direction.Axis.X?Direction.EAST
            :axis!=Direction.Axis.Y && branch!=Direction.Axis.Y?Direction.UP:Direction.SOUTH;
        for(var side:new Direction[]{normal,normal.getOpposite()}) {
            pose.pushPose();pose.translate(.5,.5,.5);
            var front=state.getValue(ThreeWayValveBlock.FACING);
            var z=new org.joml.Vector3f(front.getStepX(),front.getStepY(),front.getStepZ()).negate();
            var y=new org.joml.Vector3f(side.getStepX(),side.getStepY(),side.getStepZ());
            var x=new org.joml.Vector3f(y).cross(z);
            pose.mulPose(new org.joml.Quaternionf().setFromNormalized(new org.joml.Matrix3f().setColumn(0,x).setColumn(1,y).setColumn(2,z)));
            pose.translate(-.5,-.5,-.5);
            var target=ThreeWayValveBlock.branch(state);
            // The imported 90-degree UV places the red branch mark along local -X.
            boolean mirrored=x.dot(new org.joml.Vector3f(target.getStepX(),target.getStepY(),target.getStepZ()))>0;
            CachedBuffers.partial(mirrored?DIAL_MIRRORED:DIAL,state).light(light).overlay(overlay)
                .renderInto(pose,buffers.getBuffer(RenderType.cutoutMipped()));
            pose.translate(.5,.5,.5);
            float sign=x.dot(new org.joml.Vector3f(target.getStepX(),target.getStepY(),target.getStepZ()))>0?-1:1;
            pose.mulPose(Axis.YP.rotationDegrees(sign*90*be.dial.getValue(pt)));
            pose.translate(-.5,-.5,-.5);
            CachedBuffers.partial(POINTER,state).light(light).overlay(overlay)
                .renderInto(pose,buffers.getBuffer(RenderType.cutoutMipped()));
            pose.popPose();
        }
    }
}
