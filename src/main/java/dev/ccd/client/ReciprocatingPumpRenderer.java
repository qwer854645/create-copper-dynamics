package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.ccd.content.ReciprocatingFluidPumpBlock;
import dev.ccd.content.ReciprocatingFluidPumpBlockEntity;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public final class ReciprocatingPumpRenderer extends SafeBlockEntityRenderer<ReciprocatingFluidPumpBlockEntity> {
    private static final PartialModel BELLOWS=part("bellows"),PLATE=part("plate");
    private static PartialModel part(String name) { return PartialModel.of(ResourceLocation.fromNamespaceAndPath("ccd","block/portable_fluid_pump/"+name)); }
    public static void registerModels() {}
    public ReciprocatingPumpRenderer(BlockEntityRendererProvider.Context context) {}
    @Override protected void renderSafe(ReciprocatingFluidPumpBlockEntity be,float pt,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var state=be.getBlockState();
        var axis=((ReciprocatingFluidPumpBlock)state.getBlock()).getRotationAxis(state);
        for(var side:Direction.values())if(side.getAxis()==axis)
            KineticBlockEntityRenderer.standardKineticRotationTransform(CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF,state,side),be,light)
                .renderInto(pose,buffers.getBuffer(RenderType.solid()));
        float stroke=net.minecraft.util.Mth.lerp(pt,be.previousStroke,be.stroke);
        pose.pushPose();pose.translate(0,10/16d,0);pose.scale(1,1-stroke*.55f,1);pose.translate(0,-10/16d,0);
        CachedBuffers.partial(BELLOWS,state).light(light).overlay(overlay).renderInto(pose,buffers.getBuffer(RenderType.solid()));pose.popPose();
        CachedBuffers.partial(PLATE,state).translate(0,-stroke*2.2/16,0).light(light).overlay(overlay).renderInto(pose,buffers.getBuffer(RenderType.solid()));
    }
}
