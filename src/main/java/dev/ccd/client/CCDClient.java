package dev.ccd.client;

import dev.ccd.CopperDynamics;
import dev.ccd.content.FluidPackageItem;
import dev.ccd.content.CompositePackageItem;
import com.simibubi.create.AllPartialModels;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
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
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.content.fluids.PipeAttachmentModel;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveRenderer;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmRenderer;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmVisual;

@EventBusSubscriber(modid = CopperDynamics.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CCDClient {
    /** Register before model baking; native package entities and conveyors use these maps. */
    public static void init() {
        registerPackageModel("fluid_package");
        registerPackageModel("fluid_arm_cargo");
        registerPackageModel("composite_package");
    }

    private static void registerPackageModel(String name) {
        var item = ResourceLocation.fromNamespaceAndPath(CopperDynamics.ID, name);
        AllPartialModels.PACKAGES.put(item, PartialModel.of(item.withPrefix("item/")));
        // CCD sealed fluid carriers use the native 12 x 12 package style.
        AllPartialModels.PACKAGE_RIGGING.put(item,
            PartialModel.of(ResourceLocation.fromNamespaceAndPath("create", "item/package/rigging_12x12")));
    }

    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CopperDynamics.FLUID_PACKAGER_BE.get(), FluidPackagerRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.FLUID_GAUGE_BE.get(), FactoryPanelRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.BOOSTER_PUMP_BE.get(), BoosterPumpRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.FLUID_TRANSMITTER_BE.get(), SmartBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.REDSTONE_FLUID_VALVE_BE.get(), FluidValveRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.SMART_FAUCET_BE.get(), SmartBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.FLUID_ARM_BE.get(), ArmRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.SMART_FLUID_HATCH_BE.get(), SmartBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.SMART_FLUID_TANK_BE.get(), SmartFluidTankRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.COPPER_WATER_BASIN_BE.get(),
            com.simibubi.create.content.fluids.tank.FluidTankRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.COPPER_COLLECTION_GRATE_BE.get(), CopperCollectionGrateRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.SMART_COLLECTION_GRATE_BE.get(), CopperCollectionGrateRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.FLUID_ATOMIZER_BE.get(), com.simibubi.create.content.kinetics.fan.EncasedFanRenderer::new);
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> SimpleBlockEntityVisualizer.builder(CopperDynamics.FLUID_ATOMIZER_BE.get())
            .factory(com.simibubi.create.content.kinetics.fan.FanVisual::new).apply());
        event.enqueueWork(() -> SimpleBlockEntityVisualizer.builder(CopperDynamics.REDSTONE_FLUID_VALVE_BE.get())
            .factory(FluidValveVisual::new).apply());
        event.enqueueWork(() -> SimpleBlockEntityVisualizer.builder(CopperDynamics.FLUID_ARM_BE.get())
            .factory(ArmVisual::new).neverSkipVanillaRender().apply());
    }
    @SubscribeEvent public static void models(ModelEvent.ModifyBakingResult event) {
        for (var state : CopperDynamics.THREE_WAY_VALVE.get().getStateDefinition().getPossibleStates()) {
            var location = BlockModelShaper.stateToModelLocation(state);
            var baked = event.getModels().get(location);
            if (baked != null) event.getModels().put(location, PipeAttachmentModel.withAO(baked));
        }
        for (var state : CopperDynamics.REDSTONE_FLUID_VALVE.get().getStateDefinition().getPossibleStates()) {
            var location = BlockModelShaper.stateToModelLocation(state);
            var baked = event.getModels().get(location);
            if (baked != null) event.getModels().put(location, PipeAttachmentModel.withAO(baked));
        }
        // Reuse the native four-slot model and its active/passive panel variants.
        for (var state : CopperDynamics.FLUID_GAUGE.get().getStateDefinition().getPossibleStates()) {
            var location = BlockModelShaper.stateToModelLocation(state);
            var baked = event.getModels().get(location);
            if (baked != null) event.getModels().put(location, new FactoryPanelModel(baked));
        }
    }
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event) {
        var bucketColors = new net.neoforged.neoforge.client.model.DynamicFluidContainerModel.Colors();
        event.register((stack, layer) -> layer == 0 ? 0xffd88a60 : bucketColors.getColor(stack, layer), CopperDynamics.COPPER_BUCKET.get());
        event.register((stack, layer) -> {
            var fluid = FluidPackageItem.contents(stack);
            return layer != 0 || fluid.isEmpty() ? -1 : IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor(fluid);
        }, CopperDynamics.FLUID_PACKAGE.get(), CopperDynamics.FLUID_ARM_CARGO.get());
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
