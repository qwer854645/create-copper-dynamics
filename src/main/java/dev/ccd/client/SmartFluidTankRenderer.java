package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.ccd.content.SmartFluidTankBlockEntity;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Use Create's actual fluid-box renderer; bands are storage proportions, not mixing or density. */
public final class SmartFluidTankRenderer extends SafeBlockEntityRenderer<SmartFluidTankBlockEntity> {
    public SmartFluidTankRenderer(BlockEntityRendererProvider.Context context) {}
    @Override protected void renderSafe(SmartFluidTankBlockEntity tank, float partialTicks, PoseStack pose,
        MultiBufferSource buffer, int light, int overlay) {
        var inventory = tank.inventory;
        double denominator = Math.max(inventory.capacity(), inventory.amount());
        float bottom = .25f;
        for (int slot = 0; slot < inventory.getTanks(); slot++) {
            var fluid = inventory.getFluidInTank(slot);
            if (fluid.isEmpty()) continue;
            float top = bottom + (float) (.5 * fluid.getAmount() / denominator);
            if (top - bottom > .00001f) NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluid,
                .07f, bottom, .07f, .93f, top, .93f, buffer, pose, light, false, true);
            bottom = top;
        }
    }
}
