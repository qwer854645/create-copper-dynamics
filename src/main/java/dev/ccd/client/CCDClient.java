package dev.ccd.client;

import dev.ccd.CopperDynamics;
import dev.ccd.content.FluidPackageItem;
import dev.ccd.content.CompositePackageItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.event.ModelEvent;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelModel;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelRenderer;
import net.minecraft.client.renderer.block.BlockModelShaper;

@EventBusSubscriber(modid = CopperDynamics.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CCDClient {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CopperDynamics.FLUID_PACKAGER_BE.get(), FluidPackagerRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.FLUID_GAUGE_BE.get(), FactoryPanelRenderer::new);
    }
    @SubscribeEvent public static void models(ModelEvent.ModifyBakingResult event) {
        // Reuse the native four-slot model and its active/passive panel variants.
        for (var state : CopperDynamics.FLUID_GAUGE.get().getStateDefinition().getPossibleStates()) {
            var location = BlockModelShaper.stateToModelLocation(state);
            var baked = event.getModels().get(location);
            if (baked != null) event.getModels().put(location, new FactoryPanelModel(baked));
        }
    }
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> {
            var fluid = FluidPackageItem.contents(stack);
            return layer != 0 || fluid.isEmpty() ? -1 : IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor(fluid);
        }, CopperDynamics.FLUID_PACKAGE.get());
        event.register((stack, layer) -> {
            var fluid = dev.ccd.content.FluidStockItem.fluid(stack);
            return fluid.isEmpty() ? -1 : IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor(fluid);
        }, CopperDynamics.FLUID_STOCK.get());
        event.register((stack, layer) -> {
            var fluids = CompositePackageItem.fluids(stack);
            return layer < 0 || layer >= fluids.size() ? -1
                : IClientFluidTypeExtensions.of(fluids.get(layer).getFluid()).getTintColor(fluids.get(layer));
        }, CopperDynamics.COMPOSITE_PACKAGE.get());
    }
}
