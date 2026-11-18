package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.fluids.tank.FluidTankRenderer;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import dev.ccd.content.SmartFluidTankBlockEntity;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Use Create's actual fluid-box renderer; bands are storage proportions, not mixing or density. */
public final class SmartFluidTankRenderer extends FluidTankRenderer {
    public SmartFluidTankRenderer(BlockEntityRendererProvider.Context context) { super(context); }
    @Override protected void renderSafe(FluidTankBlockEntity entity, float partialTicks, PoseStack pose,
        MultiBufferSource buffer, int light, int overlay) {
        if (!(entity instanceof SmartFluidTankBlockEntity tank) || !tank.isController() || !tank.hasWindow()) return;
        var view = tank.inventory.snapshot();
        if (view == null) return;
        double denominator = Math.max(view.capacity(), view.amount());
        float totalHeight = tank.getHeight() - .5f;
        boolean horizontal=tank instanceof dev.ccd.content.HorizontalSmartTankBlockEntity;
        if(horizontal) totalHeight=tank.getWidth()-.5f;
        float bottom = .25f;
        for (var fluid : view.fluids()) {
            float top = bottom + (float) (totalHeight * fluid.getAmount() / denominator);
            if (top - bottom > .00001f) NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluid,
                .07f, bottom, .07f,
                horizontal && tank.getMainConnectionAxis()==net.minecraft.core.Direction.Axis.X?tank.getHeight()-.07f:tank.getWidth()-.07f,
                top, horizontal && tank.getMainConnectionAxis()==net.minecraft.core.Direction.Axis.Z?tank.getHeight()-.07f:tank.getWidth()-.07f,
                buffer, pose, light, false, true);
            bottom = top;
        }
    }
}
