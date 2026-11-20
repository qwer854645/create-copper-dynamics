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
            switch(side) {
                case DOWN -> pose.mulPose(Axis.XP.rotationDegrees(180));
                case NORTH -> pose.mulPose(Axis.XP.rotationDegrees(-90));
                case SOUTH -> pose.mulPose(Axis.XP.rotationDegrees(-270));
                case EAST -> { pose.mulPose(Axis.YP.rotationDegrees(-90));pose.mulPose(Axis.XP.rotationDegrees(-90)); }
                case WEST -> { pose.mulPose(Axis.YP.rotationDegrees(-270));pose.mulPose(Axis.XP.rotationDegrees(-90)); }
                default -> { }
            }
            pose.mulPose(Axis.YP.rotationDegrees(-90*be.dial.getValue(pt)));
            pose.translate(-.5,-.5,-.5);
            CachedBuffers.partial(POINTER,state).light(light).overlay(overlay)
                .renderInto(pose,buffers.getBuffer(RenderType.cutoutMipped()));
            pose.popPose();
        }
    }
}
