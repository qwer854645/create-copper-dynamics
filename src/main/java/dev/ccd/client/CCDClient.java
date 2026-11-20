package dev.ccd.client;

import dev.ccd.CopperDynamics;
import dev.ccd.CCDFluids;
import dev.ccd.content.FluidPackageItem;
import dev.ccd.content.CompositePackageItem;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelRenderer;
import net.minecraft.client.renderer.block.BlockModelShaper;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.content.fluids.PipeAttachmentModel;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmRenderer;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmVisual;

@EventBusSubscriber(modid = CopperDynamics.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CCDClient {
    @SubscribeEvent public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(CopperDynamics.FLUID_FILTER_MENU.get(), FluidFilterScreen::new);
    }
    /** Register before model baking; native package entities and conveyors use these maps. */
    public static void init() {
        net.createmod.ponder.foundation.PonderIndex.addPlugin(new dev.ccd.client.ponder.CCDPonderPlugin());
        AtomizerModels.init();
        FluidPackagerRenderer.registerModels();
        FluidGaugeModel.registerModels();
        FluidArmModels.registerModels();
        SmartFluidTankModels.registerModels();
        RedstoneFluidValveRenderer.registerModels();
        ThreeWayValveRenderer.registerModels();
        RedstoneFluidPumpRenderer.registerModels();
        ReciprocatingPumpRenderer.registerModels();
        PistonPumpRenderer.registerModels();
        registerPackageModel("fluid_package");
        registerPackageModel("fluid_arm_cargo");
        registerPackageModel("composite_package");
        for(var variant:dev.ccd.content.MetalPackages.VARIANTS) {
            registerPackageModel(variant.id());
            AllPartialModels.PACKAGE_RIGGING.put(ResourceLocation.fromNamespaceAndPath("ccd",variant.id()),
                PartialModel.of(ResourceLocation.fromNamespaceAndPath("create","item/package/rigging_"+variant.size())));
        }
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
        event.registerBlockEntityRenderer(CopperDynamics.FLUID_REPACKAGER_BE.get(), FluidPackagerRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.THREE_WAY_VALVE_BE.get(), ThreeWayValveRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.FLUID_GAUGE_BE.get(), FactoryPanelRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.BOOSTER_PUMP_BE.get(), BoosterPumpRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.RECIPROCATING_PUMP_BE.get(), PistonPumpRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.PORTABLE_PUMP_BE.get(), ReciprocatingPumpRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.BIDIRECTIONAL_FLUID_PUMP_BE.get(), BidirectionalFluidPumpRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.TURNING_BOOSTER_PUMP_BE.get(), BoosterPumpRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.REDSTONE_FLUID_PUMP_BE.get(), RedstoneFluidPumpRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.FLUID_TRANSMITTER_BE.get(), SmartBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.FLUID_ACCESS_PORT_BE.get(), SmartBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.FLUID_BRIDGE_BE.get(), SmartBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.REDSTONE_FLUID_VALVE_BE.get(), RedstoneFluidValveRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.FAUCET_BE.get(), FaucetRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.SMART_FAUCET_BE.get(), FaucetRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.FLUID_ARM_BE.get(), ArmRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.SMART_FLUID_HATCH_BE.get(), SmartBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.SMART_FLUID_TANK_BE.get(), SmartFluidTankRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.HORIZONTAL_SMART_TANK_BE.get(), SmartFluidTankRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.COPPER_WATER_BASIN_BE.get(),
            CopperWaterBasinRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.COPPER_WATER_SUPPLY_BE.get(), ConduitBoxRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.COPPER_COLLECTION_GRATE_BE.get(), CopperCollectionGrateRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.SMART_COLLECTION_GRATE_BE.get(), CopperCollectionGrateRenderer::new);
        event.registerBlockEntityRenderer(CopperDynamics.FLUID_ATOMIZER_BE.get(), AtomizerRenderer::new);
    }
    @SubscribeEvent public static void fluidExtensions(RegisterClientExtensionsEvent event) {
        for(var medium:dev.ccd.compat.ProcessingMedia.ALL) event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override public ResourceLocation getStillTexture() { return ResourceLocation.fromNamespaceAndPath("ccd","block/processing_medium_still"); }
            @Override public ResourceLocation getFlowingTexture() { return ResourceLocation.fromNamespaceAndPath("ccd","block/processing_medium_flow"); }
            @Override public int getTintColor() { return medium.color; }
        },medium.type.get());
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override public ResourceLocation getStillTexture() { return ResourceLocation.fromNamespaceAndPath("ccd","block/quicksand_still"); }
            @Override public ResourceLocation getFlowingTexture() { return ResourceLocation.fromNamespaceAndPath("ccd","block/quicksand_flow"); }
        },CCDFluids.QUICKSAND_TYPE.get());
        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final ResourceLocation SNOW = ResourceLocation.withDefaultNamespace("block/powder_snow");
            @Override public ResourceLocation getStillTexture() { return SNOW; }
            @Override public ResourceLocation getFlowingTexture() { return SNOW; }
        }, CCDFluids.POWDER_SNOW_TYPE.get());
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            TooltipModifier.REGISTRY.registerProvider(item -> {
                if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(CopperDynamics.ID)
                    && item != net.minecraft.world.item.Items.POWDER_SNOW_BUCKET) return null;
                return new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                    .andThen(TooltipModifier.mapNull(KineticStats.create(item)));
            });
        });
        event.enqueueWork(() -> SimpleBlockEntityVisualizer.builder(CopperDynamics.FLUID_ATOMIZER_BE.get())
            .factory(com.simibubi.create.content.kinetics.fan.FanVisual::new).neverSkipVanillaRender().apply());
        event.enqueueWork(() -> SimpleBlockEntityVisualizer.builder(CopperDynamics.FLUID_ARM_BE.get())
            .factory(ArmVisual::new).neverSkipVanillaRender().apply());
    }
    @SubscribeEvent public static void models(ModelEvent.ModifyBakingResult event) {
        for(var state:CopperDynamics.HORIZONTAL_SMART_TANK.get().getStateDefinition().getPossibleStates()) {
            var location=BlockModelShaper.stateToModelLocation(state);
            var baked=event.getModels().get(location);
            if(baked!=null) event.getModels().put(location,SmartFluidTankModels.wrapHorizontal(baked));
        }
        var canister = new net.minecraft.client.resources.model.ModelResourceLocation(
            ResourceLocation.fromNamespaceAndPath(CopperDynamics.ID, "fluid_package"), "inventory");
        var canisterModel = event.getModels().get(canister);
        if(canisterModel!=null) event.getModels().put(canister,FluidCanisterModel.install(canister.id(),canisterModel));
        for(var variant:dev.ccd.content.MetalPackages.VARIANTS) if(variant.material().equals("copper")) {
            var location=new net.minecraft.client.resources.model.ModelResourceLocation(
                ResourceLocation.fromNamespaceAndPath(CopperDynamics.ID,variant.id()),"inventory");
            var model=event.getModels().get(location);
            if(model!=null) event.getModels().put(location,FluidCanisterModel.install(location.id(),model));
        }
        var brassBox = new net.minecraft.client.resources.model.ModelResourceLocation(
            ResourceLocation.fromNamespaceAndPath(CopperDynamics.ID, "composite_package"), "inventory");
        var brassBoxModel = event.getModels().get(brassBox);
        if(brassBoxModel!=null) event.getModels().put(brassBox,BrassBoxModel.install(brassBox.id(),brassBoxModel));
        for(var variant:dev.ccd.content.MetalPackages.VARIANTS) if(variant.material().equals("brass")) {
            var location=new net.minecraft.client.resources.model.ModelResourceLocation(
                ResourceLocation.fromNamespaceAndPath(CopperDynamics.ID,variant.id()),"inventory");
            var model=event.getModels().get(location);
            if(model!=null) event.getModels().put(location,BrassBoxModel.install(location.id(),model));
        }
        var cargo = new net.minecraft.client.resources.model.ModelResourceLocation(
            ResourceLocation.fromNamespaceAndPath(CopperDynamics.ID, "fluid_arm_cargo"), "inventory");
        var cargoModel = event.getModels().get(cargo);
        if (cargoModel != null) event.getModels().put(cargo, new FluidArmCargoModel(cargoModel));
        for (var state : CopperDynamics.SMART_FLUID_TANK.get().getStateDefinition().getPossibleStates()) {
            var location = BlockModelShaper.stateToModelLocation(state);
            var baked = event.getModels().get(location);
            if (baked != null) event.getModels().put(location, SmartFluidTankModels.wrap(baked));
        }
        // Three-way valve multipart models already include native rims/connectors and status stamps.
        // A PipeAttachmentModel wrapper would draw duplicate coplanar mouths over those stamps.
        for (var state : CopperDynamics.REDSTONE_FLUID_VALVE.get().getStateDefinition().getPossibleStates()) {
            var location = BlockModelShaper.stateToModelLocation(state);
            var baked = event.getModels().get(location);
            // The inset sealed copper faces need flat lighting instead of interpolated corner AO.
            if (baked != null) event.getModels().put(location, PipeAttachmentModel.withoutAO(baked));
        }
        // Reuse the native four-slot model and its active/passive panel variants.
        for (var state : CopperDynamics.FLUID_GAUGE.get().getStateDefinition().getPossibleStates()) {
            var location = BlockModelShaper.stateToModelLocation(state);
            var baked = event.getModels().get(location);
            if (baked != null) event.getModels().put(location, new FluidGaugeModel(baked,
                event.getTextureGetter().apply(new net.minecraft.client.resources.model.Material(
                    net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS,
                    ResourceLocation.fromNamespaceAndPath(CopperDynamics.ID, "block/fluid_gauge/factory_panel"))),
                event.getTextureGetter().apply(new net.minecraft.client.resources.model.Material(
                    net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS,
                    ResourceLocation.fromNamespaceAndPath(CopperDynamics.ID, "block/fluid_gauge/factory_panel_packager_mode")))));
        }
    }
    @SubscribeEvent public static void modelsReady(ModelEvent.BakingCompleted event) {
        FluidCanisterModel.rebuildTransportVariants();
        BrassBoxModel.rebuildTransportVariants();
    }
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event) {
        var bucketColors = new net.neoforged.neoforge.client.model.DynamicFluidContainerModel.Colors();
        event.register(bucketColors, CopperDynamics.COPPER_BUCKET.get(), CopperDynamics.BRASS_BUCKET.get());
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
