package dev.ccd;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import dev.ccd.content.BoosterPumpBlock;
import dev.ccd.content.BoosterPumpBlockEntity;
import dev.ccd.content.FluidTransmitterBlock;
import dev.ccd.content.FluidTransmitterBlockEntity;
import dev.ccd.content.RedstoneFluidValveBlock;
import dev.ccd.content.RedstoneFluidValveBlockEntity;
import dev.ccd.content.ThreeWayValveBlock;
import dev.ccd.content.ThreeWayValveBlockEntity;
import dev.ccd.content.FaucetBlock;
import dev.ccd.content.FaucetBlockEntity;
import dev.ccd.content.SmartFaucetBlock;
import dev.ccd.content.SmartFaucetBlockEntity;
import dev.ccd.content.FluidArmBlock;
import dev.ccd.content.FluidArmBlockEntity;
import dev.ccd.content.FluidArmItem;
import dev.ccd.content.FluidArmCargoItem;
import dev.ccd.content.FluidHatchBlock;
import dev.ccd.content.FluidHatchBlockEntity;
import dev.ccd.content.SmartFluidHatchBlock;
import dev.ccd.content.SmartFluidHatchBlockEntity;
import dev.ccd.content.SmartFluidTankBlock;
import dev.ccd.content.SmartFluidTankBlockEntity;
import dev.ccd.content.CopperWaterBasinBlock;
import dev.ccd.content.CopperKitchenSinkBlock;
import dev.ccd.content.CopperWaterBasinBlockEntity;
import dev.ccd.content.CopperWaterSupplyBlock;
import dev.ccd.content.CopperWaterSupplyBlockEntity;
import dev.ccd.content.CopperCollectionGrateBlock;
import dev.ccd.content.CopperCollectionGrateBlockEntity;
import dev.ccd.content.SmartCollectionGrateBlock;
import dev.ccd.content.SmartCollectionGrateBlockEntity;
import dev.ccd.content.CopperBucketItem;
import dev.ccd.content.FluidAtomizerBlock;
import dev.ccd.content.FluidAtomizerBlockEntity;
import dev.ccd.atomizing.AtomizingRecipe;
import dev.ccd.atomizing.AtomizingFanType;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import dev.ccd.logistics.FluidArmPoint;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import dev.ccd.client.CCDClient;
import net.createmod.catnip.platform.CatnipServices;
import dev.ccd.content.FluidCargo;
import dev.ccd.content.FluidPackageItem;
import dev.ccd.content.FluidPackagerBlock;
import dev.ccd.content.FluidPackagerBlockEntity;
import dev.ccd.content.FluidStockItem;
import dev.ccd.content.FluidGaugeBlock;
import dev.ccd.content.FluidGaugeBlockEntity;
import dev.ccd.content.CompositeFluids;
import dev.ccd.content.CompositePackageItem;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(CopperDynamics.ID)
public final class CopperDynamics {
    public static final String ID = "ccd";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ID);
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ID);
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.world.item.component.CustomData>> FLUID_FILTER_DATA =
        COMPONENTS.register("fluid_filter", () -> DataComponentType.<net.minecraft.world.item.component.CustomData>builder()
            .persistent(net.minecraft.world.item.component.CustomData.CODEC)
            .networkSynchronized(net.minecraft.world.item.component.CustomData.STREAM_CODEC).build());
    public static final DeferredHolder<Item, dev.ccd.content.FluidFilterItem> FLUID_FILTER =
        ITEMS.register("fluid_filter", () -> new dev.ccd.content.FluidFilterItem(new Item.Properties(), false));
    public static final DeferredHolder<Item, dev.ccd.content.FluidFilterItem> FLUID_ATTRIBUTE_FILTER =
        ITEMS.register("fluid_attribute_filter", () -> new dev.ccd.content.FluidFilterItem(new Item.Properties(), true));
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<dev.ccd.content.FluidFilterMenu>> FLUID_FILTER_MENU =
        MENUS.register("fluid_filter", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(
            (id, inventory, buffer) -> new dev.ccd.content.FluidFilterMenu(id, inventory, ItemStack.STREAM_CODEC.decode(buffer))));
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final DeferredRegister<ArmInteractionPointType> ARM_POINTS = DeferredRegister.create(CreateRegistries.ARM_INTERACTION_POINT_TYPE, ID);
    public static final DeferredRegister<FanProcessingType> FAN_TYPES = DeferredRegister.create(CreateRegistries.FAN_PROCESSING_TYPE, ID);
    public static final DeferredHolder<FanProcessingType, AtomizingFanType> ATOMIZING_FAN_TYPE = FAN_TYPES.register("atomizing", AtomizingFanType::new);
    public static final DeferredHolder<FanProcessingType, dev.ccd.atomizing.SandblastingType> SANDBLASTING =
        FAN_TYPES.register("sandblasting", dev.ccd.atomizing.SandblastingType::new);
    public static final DeferredHolder<FanProcessingType,dev.ccd.atomizing.BulkGluingType> BULK_GLUING=FAN_TYPES.register("bulk_gluing",dev.ccd.atomizing.BulkGluingType::new);
    public static final DeferredHolder<net.minecraft.world.level.block.Block,net.minecraft.world.level.block.LiquidBlock> SLIME_BLOCK=BLOCKS.register("slime",()->new net.minecraft.world.level.block.LiquidBlock(CCDFluids.SLIME.get(),BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WATER)));
    public static final DeferredHolder<Item,net.minecraft.world.item.BucketItem> SLIME_BUCKET=ITEMS.register("slime_bucket",()->new net.minecraft.world.item.BucketItem(CCDFluids.SLIME.get(),new Item.Properties().stacksTo(1).craftRemainder(net.minecraft.world.item.Items.BUCKET)));
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, ID);
    public static final DeferredHolder<RecipeType<?>, RecipeType<AtomizingRecipe>> ATOMIZING_TYPE = RECIPE_TYPES.register("atomizing",
        () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(ID, "atomizing")));
    public static final DeferredHolder<RecipeSerializer<?>, AtomizingRecipe.Serializer> ATOMIZING_SERIALIZER =
        RECIPE_SERIALIZERS.register("atomizing", AtomizingRecipe.Serializer::new);
    public static final DeferredHolder<ArmInteractionPointType, FluidArmPoint.Type> FLUID_ARM_POINT =
        ARM_POINTS.register("fluid_arm_point", FluidArmPoint.Type::new);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FluidCargo>> FLUID_CARGO =
        COMPONENTS.register("fluid_cargo", () -> DataComponentType.<FluidCargo>builder()
            .persistent(FluidCargo.CODEC).networkSynchronized(FluidCargo.STREAM_CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompositeFluids>> COMPOSITE_FLUIDS =
        COMPONENTS.register("composite_fluids", () -> DataComponentType.<CompositeFluids>builder()
            .persistent(CompositeFluids.CODEC).networkSynchronized(CompositeFluids.STREAM_CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> RECOVERED =
        COMPONENTS.register("recovered", () -> DataComponentType.<Boolean>builder()
            .persistent(com.mojang.serialization.Codec.BOOL)
            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> COPPER_BUCKET_FLUID =
        COMPONENTS.register("copper_bucket_fluid", () -> DataComponentType.<SimpleFluidContent>builder()
            .persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC).build());
    public static final DeferredHolder<Item, CopperBucketItem> COPPER_BUCKET =
        ITEMS.register("copper_bucket", () -> new CopperBucketItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, CopperBucketItem> BRASS_BUCKET =
        ITEMS.register("brass_bucket", () -> new CopperBucketItem(new Item.Properties().stacksTo(1), CopperBucketItem.BRASS_CAPACITY));
    public static final DeferredHolder<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.LiquidBlock> QUICKSAND_BLOCK =
        BLOCKS.register("quicksand", () -> new net.minecraft.world.level.block.LiquidBlock(CCDFluids.QUICKSAND.get(),
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WATER)));
    public static final DeferredHolder<Item, net.minecraft.world.item.BucketItem> QUICKSAND_BUCKET =
        ITEMS.register("quicksand_bucket", () -> new net.minecraft.world.item.BucketItem(CCDFluids.QUICKSAND.get(),
            new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1)));
    public static final DeferredHolder<Item, FluidPackageItem> FLUID_PACKAGE =
        ITEMS.register("fluid_package", () -> new FluidPackageItem(new Item.Properties().stacksTo(1)));
    // Internal native-arm cargo representation; deliberately absent from creative tabs and recipes.
    public static final DeferredHolder<Item, FluidArmCargoItem> FLUID_ARM_CARGO =
        ITEMS.register("fluid_arm_cargo", () -> new FluidArmCargoItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, CompositePackageItem> COMPOSITE_PACKAGE =
        ITEMS.register("composite_package", () -> new CompositePackageItem(new Item.Properties()));
    public static final DeferredHolder<Item, FluidStockItem> FLUID_STOCK =
        ITEMS.register("fluid_stock", () -> new FluidStockItem(new Item.Properties()));
    public static final DeferredHolder<net.minecraft.world.level.block.Block, FluidGaugeBlock> FLUID_GAUGE =
        BLOCKS.register("fluid_gauge", () -> new FluidGaugeBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE).strength(1.0F).noOcclusion()));
    public static final DeferredHolder<Item, FactoryPanelBlockItem> FLUID_GAUGE_ITEM =
        ITEMS.register("fluid_gauge", () -> new FactoryPanelBlockItem(FLUID_GAUGE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidGaugeBlockEntity>> FLUID_GAUGE_BE =
        BLOCK_ENTITIES.register("fluid_gauge", () -> BlockEntityType.Builder.of(
            FluidGaugeBlockEntity::new, FLUID_GAUGE.get()).build(null));
    public static final DeferredHolder<net.minecraft.world.level.block.Block, FluidPackagerBlock> FLUID_PACKAGER =
        BLOCKS.register("fluid_packager", () -> new FluidPackagerBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE).strength(3.0F).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredHolder<net.minecraft.world.level.block.Block, dev.ccd.content.FluidRepackagerBlock> FLUID_REPACKAGER =
        BLOCKS.register("fluid_repackager", () -> new dev.ccd.content.FluidRepackagerBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE).strength(3.0F).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredHolder<Item, BlockItem> FLUID_REPACKAGER_ITEM =
        ITEMS.register("fluid_repackager", () -> new BlockItem(FLUID_REPACKAGER.get(),new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.ccd.content.FluidRepackagerBlockEntity>> FLUID_REPACKAGER_BE =
        BLOCK_ENTITIES.register("fluid_repackager", () -> BlockEntityType.Builder.of(
            dev.ccd.content.FluidRepackagerBlockEntity::new,FLUID_REPACKAGER.get()).build(null));
    public static final DeferredHolder<Item, BlockItem> FLUID_PACKAGER_ITEM =
        ITEMS.register("fluid_packager", () -> new BlockItem(FLUID_PACKAGER.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidPackagerBlockEntity>> FLUID_PACKAGER_BE =
        BLOCK_ENTITIES.register("fluid_packager", () -> BlockEntityType.Builder.of(
            FluidPackagerBlockEntity::new, FLUID_PACKAGER.get()).build(null));

    public static final DeferredHolder<net.minecraft.world.level.block.Block, BoosterPumpBlock> BOOSTER_PUMP =
        BLOCKS.register("booster_pump", () -> new BoosterPumpBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE).strength(3.0F).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredHolder<net.minecraft.world.level.block.Block, dev.ccd.content.RedstoneFluidPumpBlock> REDSTONE_FLUID_PUMP =
        BLOCKS.register("redstone_fluid_pump",()->new dev.ccd.content.RedstoneFluidPumpBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE).strength(3).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredHolder<Item,BlockItem> REDSTONE_FLUID_PUMP_ITEM =
        ITEMS.register("redstone_fluid_pump",()->new BlockItem(REDSTONE_FLUID_PUMP.get(),new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<dev.ccd.content.RedstoneFluidPumpBlockEntity>> REDSTONE_FLUID_PUMP_BE =
        BLOCK_ENTITIES.register("redstone_fluid_pump",()->BlockEntityType.Builder.of(dev.ccd.content.RedstoneFluidPumpBlockEntity::new,REDSTONE_FLUID_PUMP.get()).build(null));
    public static final DeferredHolder<Item, BlockItem> BOOSTER_PUMP_ITEM =
        ITEMS.register("booster_pump", () -> new BlockItem(BOOSTER_PUMP.get(), new Item.Properties()));
    public static final DeferredHolder<net.minecraft.world.level.block.Block,dev.ccd.content.BufferPumpBlock> BUFFER_PUMP=BLOCKS.register("buffer_pump",()->new dev.ccd.content.BufferPumpBlock(BlockBehaviour.Properties.of().strength(3).noOcclusion()));
    public static final DeferredHolder<Item,BlockItem> BUFFER_PUMP_ITEM=ITEMS.register("buffer_pump",()->new dev.ccd.content.BufferPumpBlockItem(BUFFER_PUMP.get(),new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<dev.ccd.content.BufferPumpBlockEntity>> BUFFER_PUMP_BE=BLOCK_ENTITIES.register("buffer_pump",()->BlockEntityType.Builder.of(dev.ccd.content.BufferPumpBlockEntity::new,BUFFER_PUMP.get()).build(null));
    public static final DeferredHolder<net.minecraft.world.level.block.Block,dev.ccd.content.CreativeDeliveryTankBlock> CREATIVE_DELIVERY_TANK =
        BLOCKS.register("creative_delivery_fluid_tank",()->new dev.ccd.content.CreativeDeliveryTankBlock(BlockBehaviour.Properties.of().strength(3).noOcclusion()));
    public static final DeferredHolder<Item,BlockItem> CREATIVE_DELIVERY_TANK_ITEM=ITEMS.register("creative_delivery_fluid_tank",()->new BlockItem(CREATIVE_DELIVERY_TANK.get(),new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<dev.ccd.content.CreativeDeliveryTankBlockEntity>> CREATIVE_DELIVERY_TANK_BE=
        BLOCK_ENTITIES.register("creative_delivery_fluid_tank",()->BlockEntityType.Builder.of(dev.ccd.content.CreativeDeliveryTankBlockEntity::new,CREATIVE_DELIVERY_TANK.get()).build(null));
    public static final DeferredHolder<net.minecraft.world.level.block.Block,dev.ccd.content.PortableFluidPumpBlock> PORTABLE_PUMP =
        BLOCKS.register("portable_fluid_pump",()->new dev.ccd.content.PortableFluidPumpBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(3).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredHolder<Item,BlockItem> PORTABLE_PUMP_ITEM = ITEMS.register("portable_fluid_pump",()->new BlockItem(PORTABLE_PUMP.get(),new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<dev.ccd.content.PortableFluidPumpBlockEntity>> PORTABLE_PUMP_BE =
        BLOCK_ENTITIES.register("portable_fluid_pump",()->BlockEntityType.Builder.of(dev.ccd.content.PortableFluidPumpBlockEntity::new,PORTABLE_PUMP.get()).build(null));
    public static final DeferredHolder<net.minecraft.world.level.block.Block, dev.ccd.content.ReciprocatingFluidPumpBlock> RECIPROCATING_PUMP =
        BLOCKS.register("reciprocating_fluid_pump",()->new dev.ccd.content.ReciprocatingFluidPumpBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE).strength(3).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredHolder<Item,BlockItem> RECIPROCATING_PUMP_ITEM =
        ITEMS.register("reciprocating_fluid_pump",()->new BlockItem(RECIPROCATING_PUMP.get(),new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<dev.ccd.content.ReciprocatingFluidPumpBlockEntity>> RECIPROCATING_PUMP_BE =
        BLOCK_ENTITIES.register("reciprocating_fluid_pump",()->BlockEntityType.Builder.of(dev.ccd.content.ReciprocatingFluidPumpBlockEntity::new,RECIPROCATING_PUMP.get()).build(null));
    public static final DeferredHolder<net.minecraft.world.level.block.Block, dev.ccd.content.BidirectionalFluidPumpBlock> BIDIRECTIONAL_FLUID_PUMP =
        BLOCKS.register("bidirectional_fluid_pump", () -> new dev.ccd.content.BidirectionalFluidPumpBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE).strength(3).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredHolder<Item, BlockItem> BIDIRECTIONAL_FLUID_PUMP_ITEM =
        ITEMS.register("bidirectional_fluid_pump", () -> new BlockItem(BIDIRECTIONAL_FLUID_PUMP.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.ccd.content.BidirectionalFluidPumpBlockEntity>> BIDIRECTIONAL_FLUID_PUMP_BE =
        BLOCK_ENTITIES.register("bidirectional_fluid_pump", () -> BlockEntityType.Builder.of(
            dev.ccd.content.BidirectionalFluidPumpBlockEntity::new, BIDIRECTIONAL_FLUID_PUMP.get()).build(null));
    public static final DeferredHolder<net.minecraft.world.level.block.Block, dev.ccd.content.TurningBoosterPumpBlock> TURNING_BOOSTER_PUMP =
        BLOCKS.register("turning_booster_pump", () -> new dev.ccd.content.TurningBoosterPumpBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE).strength(3).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredHolder<Item, BlockItem> TURNING_BOOSTER_PUMP_ITEM =
        ITEMS.register("turning_booster_pump", () -> new BlockItem(TURNING_BOOSTER_PUMP.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.ccd.content.TurningBoosterPumpBlockEntity>> TURNING_BOOSTER_PUMP_BE =
        BLOCK_ENTITIES.register("turning_booster_pump", () -> BlockEntityType.Builder.of(
            dev.ccd.content.TurningBoosterPumpBlockEntity::new, TURNING_BOOSTER_PUMP.get(), CopperDynamics.VERTICAL_TURNING_BOOSTER_PUMP.get()).build(null));
    public static final DeferredHolder<net.minecraft.world.level.block.Block, dev.ccd.content.VerticalTurningBoosterPumpBlock> VERTICAL_TURNING_BOOSTER_PUMP =
        BLOCKS.register("vertical_turning_booster_pump", () -> new dev.ccd.content.VerticalTurningBoosterPumpBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE).strength(3).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredHolder<Item, BlockItem> VERTICAL_TURNING_BOOSTER_PUMP_ITEM =
        ITEMS.register("vertical_turning_booster_pump", () -> new BlockItem(VERTICAL_TURNING_BOOSTER_PUMP.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BoosterPumpBlockEntity>> BOOSTER_PUMP_BE =
        BLOCK_ENTITIES.register("booster_pump", () -> BlockEntityType.Builder.of(
            BoosterPumpBlockEntity::new, BOOSTER_PUMP.get()).build(null));

    public static final DeferredHolder<net.minecraft.world.level.block.Block, FluidTransmitterBlock> FLUID_TRANSMITTER =
        BLOCKS.register("fluid_transmitter", () -> new FluidTransmitterBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE).strength(2.0F).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredHolder<net.minecraft.world.level.block.Block, dev.ccd.content.FlowMeterBlock> FLOW_METER =
        BLOCKS.register("flow_meter",()->new dev.ccd.content.FlowMeterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(2).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredHolder<Item,BlockItem> FLOW_METER_ITEM = ITEMS.register("flow_meter",()->new BlockItem(FLOW_METER.get(),new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<dev.ccd.content.FlowMeterBlockEntity>> FLOW_METER_BE =
        BLOCK_ENTITIES.register("flow_meter",()->BlockEntityType.Builder.of(dev.ccd.content.FlowMeterBlockEntity::new,FLOW_METER.get()).build(null));
    public static final DeferredHolder<net.minecraft.world.level.block.Block,dev.ccd.content.OmniPumpBlock> OMNI_PUMP =
        BLOCKS.register("omni_pump",()->new dev.ccd.content.OmniPumpBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(3).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredHolder<Item,BlockItem> OMNI_PUMP_ITEM=ITEMS.register("omni_pump",()->new BlockItem(OMNI_PUMP.get(),new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<dev.ccd.content.OmniPumpBlockEntity>> OMNI_PUMP_BE=
        BLOCK_ENTITIES.register("omni_pump",()->BlockEntityType.Builder.of(dev.ccd.content.OmniPumpBlockEntity::new,OMNI_PUMP.get()).build(null));
    public static final DeferredHolder<Item, BlockItem> FLUID_TRANSMITTER_ITEM =
        ITEMS.register("fluid_transmitter", () -> new BlockItem(FLUID_TRANSMITTER.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidTransmitterBlockEntity>> FLUID_TRANSMITTER_BE =
        BLOCK_ENTITIES.register("fluid_transmitter", () -> BlockEntityType.Builder.of(
            FluidTransmitterBlockEntity::new, FLUID_TRANSMITTER.get()).build(null));

    public static final DeferredHolder<net.minecraft.world.level.block.Block, RedstoneFluidValveBlock> REDSTONE_FLUID_VALVE =
        BLOCKS.register("redstone_fluid_valve", () -> new RedstoneFluidValveBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE).strength(3.0F).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredHolder<Item, BlockItem> REDSTONE_FLUID_VALVE_ITEM =
        ITEMS.register("redstone_fluid_valve", () -> new BlockItem(REDSTONE_FLUID_VALVE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RedstoneFluidValveBlockEntity>> REDSTONE_FLUID_VALVE_BE =
        BLOCK_ENTITIES.register("redstone_fluid_valve", () -> BlockEntityType.Builder.of(
            RedstoneFluidValveBlockEntity::new, REDSTONE_FLUID_VALVE.get()).build(null));

    public static final DeferredHolder<net.minecraft.world.level.block.Block, ThreeWayValveBlock> THREE_WAY_VALVE =
        BLOCKS.register("three_way_valve", () -> new ThreeWayValveBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE).strength(3.0F).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredHolder<Item, BlockItem> THREE_WAY_VALVE_ITEM =
        ITEMS.register("three_way_valve", () -> new BlockItem(THREE_WAY_VALVE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThreeWayValveBlockEntity>> THREE_WAY_VALVE_BE =
        BLOCK_ENTITIES.register("three_way_valve", () -> BlockEntityType.Builder.of(
            ThreeWayValveBlockEntity::new, THREE_WAY_VALVE.get()).build(null));

    public static final DeferredHolder<net.minecraft.world.level.block.Block, FaucetBlock> FAUCET =
        BLOCKS.register("faucet", () -> new FaucetBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE).strength(2.0F).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredHolder<Item, BlockItem> FAUCET_ITEM =
        ITEMS.register("faucet", () -> new BlockItem(FAUCET.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FaucetBlockEntity>> FAUCET_BE =
        BLOCK_ENTITIES.register("faucet", () -> BlockEntityType.Builder.of(FaucetBlockEntity::new, FAUCET.get()).build(null));

    public static final DeferredHolder<net.minecraft.world.level.block.Block, SmartFaucetBlock> SMART_FAUCET =
        BLOCKS.register("smart_faucet", () -> new SmartFaucetBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW).strength(2.0F).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredHolder<Item, BlockItem> SMART_FAUCET_ITEM =
        ITEMS.register("smart_faucet", () -> new BlockItem(SMART_FAUCET.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmartFaucetBlockEntity>> SMART_FAUCET_BE =
        BLOCK_ENTITIES.register("smart_faucet", () -> BlockEntityType.Builder.of(
            SmartFaucetBlockEntity::new, SMART_FAUCET.get()).build(null));

    public static final DeferredHolder<net.minecraft.world.level.block.Block, FluidArmBlock> FLUID_ARM =
        BLOCKS.register("fluid_arm", () -> new FluidArmBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW).strength(3.0F).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredHolder<Item, FluidArmItem> FLUID_ARM_ITEM =
        ITEMS.register("fluid_arm", () -> new FluidArmItem(FLUID_ARM.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidArmBlockEntity>> FLUID_ARM_BE =
        BLOCK_ENTITIES.register("fluid_arm", () -> BlockEntityType.Builder.of(FluidArmBlockEntity::new, FLUID_ARM.get()).build(null));

    public static final DeferredHolder<net.minecraft.world.level.block.Block, FluidHatchBlock> FLUID_HATCH =
        BLOCKS.register("fluid_hatch", () -> new FluidHatchBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE).strength(3.0F).requiresCorrectToolForDrops().noOcclusion()
            .sound(net.minecraft.world.level.block.SoundType.NETHERITE_BLOCK)));
    public static final DeferredHolder<Item, BlockItem> FLUID_HATCH_ITEM =
        ITEMS.register("fluid_hatch", () -> new BlockItem(FLUID_HATCH.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidHatchBlockEntity>> FLUID_HATCH_BE =
        BLOCK_ENTITIES.register("fluid_hatch", () -> BlockEntityType.Builder.of(FluidHatchBlockEntity::new, FLUID_HATCH.get()).build(null));
    public static final DeferredHolder<net.minecraft.world.level.block.Block, SmartFluidHatchBlock> SMART_FLUID_HATCH =
        BLOCKS.register("smart_fluid_hatch", () -> new SmartFluidHatchBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW).strength(3.0F).requiresCorrectToolForDrops().noOcclusion()
            .sound(net.minecraft.world.level.block.SoundType.NETHERITE_BLOCK)));
    public static final DeferredHolder<Item, BlockItem> SMART_FLUID_HATCH_ITEM =
        ITEMS.register("smart_fluid_hatch", () -> new BlockItem(SMART_FLUID_HATCH.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmartFluidHatchBlockEntity>> SMART_FLUID_HATCH_BE =
        BLOCK_ENTITIES.register("smart_fluid_hatch", () -> BlockEntityType.Builder.of(
            SmartFluidHatchBlockEntity::new, SMART_FLUID_HATCH.get()).build(null));

    public static final DeferredHolder<net.minecraft.world.level.block.Block, SmartFluidTankBlock> SMART_FLUID_TANK =
        BLOCKS.register("smart_fluid_tank", () -> new SmartFluidTankBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW).strength(3.0F).requiresCorrectToolForDrops().noOcclusion()
            .sound(net.minecraft.world.level.block.SoundType.METAL)));
    public static final DeferredHolder<Item, BlockItem> SMART_FLUID_TANK_ITEM =
        ITEMS.register("smart_fluid_tank", () -> new com.simibubi.create.content.fluids.tank.FluidTankItem(
            SMART_FLUID_TANK.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmartFluidTankBlockEntity>> SMART_FLUID_TANK_BE =
        BLOCK_ENTITIES.register("smart_fluid_tank", () -> BlockEntityType.Builder.of(
            SmartFluidTankBlockEntity::new, SMART_FLUID_TANK.get()).build(null));

    public static final DeferredHolder<net.minecraft.world.level.block.Block, CopperWaterBasinBlock> COPPER_WATER_BASIN =
        BLOCKS.register("copper_water_basin", () -> new CopperKitchenSinkBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE).strength(3.0F).requiresCorrectToolForDrops().noOcclusion()
            .sound(net.minecraft.world.level.block.SoundType.COPPER)));
    public static final DeferredHolder<Item, BlockItem> COPPER_WATER_BASIN_ITEM =
        ITEMS.register("copper_water_basin", () -> new BlockItem(COPPER_WATER_BASIN.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CopperWaterBasinBlockEntity>> COPPER_WATER_BASIN_BE =
        BLOCK_ENTITIES.register("copper_water_basin", () -> BlockEntityType.Builder.of(
            CopperWaterBasinBlockEntity::new, COPPER_WATER_BASIN.get()).build(null));

    public static final DeferredHolder<net.minecraft.world.level.block.Block, CopperWaterSupplyBlock> COPPER_WATER_SUPPLY =
        BLOCKS.register("copper_water_supply", () -> new CopperWaterSupplyBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE).strength(3.0F).requiresCorrectToolForDrops().noOcclusion()
            .sound(net.minecraft.world.level.block.SoundType.COPPER)));
    public static final DeferredHolder<Item, BlockItem> COPPER_WATER_SUPPLY_ITEM =
        ITEMS.register("copper_water_supply", () -> new BlockItem(COPPER_WATER_SUPPLY.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CopperWaterSupplyBlockEntity>> COPPER_WATER_SUPPLY_BE =
        BLOCK_ENTITIES.register("copper_water_supply", () -> BlockEntityType.Builder.of(
            CopperWaterSupplyBlockEntity::new, COPPER_WATER_SUPPLY.get()).build(null));

    public static final DeferredHolder<net.minecraft.world.level.block.Block, CopperCollectionGrateBlock> COPPER_COLLECTION_GRATE =
        BLOCKS.register("copper_collection_grate", () -> new CopperCollectionGrateBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE).strength(3.0F).requiresCorrectToolForDrops().noOcclusion()
            .sound(net.minecraft.world.level.block.SoundType.COPPER)));
    public static final DeferredHolder<Item, BlockItem> COPPER_COLLECTION_GRATE_ITEM =
        ITEMS.register("copper_collection_grate", () -> new BlockItem(COPPER_COLLECTION_GRATE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CopperCollectionGrateBlockEntity>> COPPER_COLLECTION_GRATE_BE =
        BLOCK_ENTITIES.register("copper_collection_grate", () -> BlockEntityType.Builder.of(
            CopperCollectionGrateBlockEntity::new, COPPER_COLLECTION_GRATE.get()).build(null));

    public static final DeferredHolder<net.minecraft.world.level.block.Block, SmartCollectionGrateBlock> SMART_COLLECTION_GRATE =
        BLOCKS.register("smart_collection_grate", () -> new SmartCollectionGrateBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW).strength(3.0F).requiresCorrectToolForDrops().noOcclusion()
            .sound(net.minecraft.world.level.block.SoundType.COPPER)));
    public static final DeferredHolder<Item, BlockItem> SMART_COLLECTION_GRATE_ITEM =
        ITEMS.register("smart_collection_grate", () -> new BlockItem(SMART_COLLECTION_GRATE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmartCollectionGrateBlockEntity>> SMART_COLLECTION_GRATE_BE =
        BLOCK_ENTITIES.register("smart_collection_grate", () -> BlockEntityType.Builder.of(
            SmartCollectionGrateBlockEntity::new, SMART_COLLECTION_GRATE.get()).build(null));

    public static final DeferredHolder<net.minecraft.world.level.block.Block, FluidAtomizerBlock> FLUID_ATOMIZER =
        BLOCKS.register("fluid_atomizer", () -> new FluidAtomizerBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE).strength(3.0F).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredHolder<Item, BlockItem> FLUID_ATOMIZER_ITEM =
        ITEMS.register("fluid_atomizer", () -> new BlockItem(FLUID_ATOMIZER.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidAtomizerBlockEntity>> FLUID_ATOMIZER_BE =
        BLOCK_ENTITIES.register("fluid_atomizer", () -> BlockEntityType.Builder.of(FluidAtomizerBlockEntity::new, FLUID_ATOMIZER.get()).build(null));

    public static final DeferredHolder<net.minecraft.world.level.block.Block, dev.ccd.content.FluidAccessBlock> FLUID_ACCESS_PORT =
        BLOCKS.register("fluid_access_port", () -> new dev.ccd.content.FluidAccessBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE).strength(2).requiresCorrectToolForDrops(), false));
    public static final DeferredHolder<net.minecraft.world.level.block.Block, dev.ccd.content.FluidAccessBlock> FLUID_BRIDGE =
        BLOCKS.register("fluid_bridge", () -> new dev.ccd.content.FluidAccessBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW).strength(2).requiresCorrectToolForDrops(), true));
    public static final DeferredHolder<Item, BlockItem> FLUID_ACCESS_PORT_ITEM =
        ITEMS.register("fluid_access_port", () -> new BlockItem(FLUID_ACCESS_PORT.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> FLUID_BRIDGE_ITEM =
        ITEMS.register("fluid_bridge", () -> new BlockItem(FLUID_BRIDGE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.ccd.content.FluidAccessBlockEntity>> FLUID_ACCESS_PORT_BE =
        BLOCK_ENTITIES.register("fluid_access_port", () -> BlockEntityType.Builder.of(
            (pos,state) -> new dev.ccd.content.FluidAccessBlockEntity(CopperDynamics.FLUID_ACCESS_PORT_BE.get(),pos,state), FLUID_ACCESS_PORT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.ccd.content.FluidAccessBlockEntity>> FLUID_BRIDGE_BE =
        BLOCK_ENTITIES.register("fluid_bridge", () -> BlockEntityType.Builder.of(
            (pos,state) -> new dev.ccd.content.FluidAccessBlockEntity(CopperDynamics.FLUID_BRIDGE_BE.get(),pos,state), FLUID_BRIDGE.get()).build(null));

    public static final DeferredHolder<net.minecraft.world.level.block.Block, dev.ccd.content.HorizontalSmartTankBlock> HORIZONTAL_SMART_TANK =
        BLOCKS.register("horizontal_smart_fluid_tank", () -> new dev.ccd.content.HorizontalSmartTankBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW).strength(3).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredHolder<Item, BlockItem> HORIZONTAL_SMART_TANK_ITEM =
        ITEMS.register("horizontal_smart_fluid_tank", () -> new dev.ccd.content.HorizontalSmartTankItem(HORIZONTAL_SMART_TANK.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.ccd.content.HorizontalSmartTankBlockEntity>> HORIZONTAL_SMART_TANK_BE =
        BLOCK_ENTITIES.register("horizontal_smart_fluid_tank", () -> BlockEntityType.Builder.of(
            dev.ccd.content.HorizontalSmartTankBlockEntity::new, HORIZONTAL_SMART_TANK.get()).build(null));

    public CopperDynamics(IEventBus bus, ModContainer container) {
        dev.ccd.content.MetalPackages.register();
        container.registerConfig(ModConfig.Type.SERVER, CCDConfig.SPEC);
        CCDFluids.register(bus);
        dev.ccd.compat.ProcessingMedia.register(bus);
        CatnipServices.PLATFORM.executeOnClientOnly(() -> CCDClient::init);
        ITEMS.register(bus);
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        COMPONENTS.register(bus);
        MENUS.register(bus);
        bus.addListener(dev.ccd.filter.FluidFilterPacket::register);
        ARM_POINTS.register(bus);
        FAN_TYPES.register(bus);
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
        TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ccd"))
            .icon(() -> new ItemStack(FLUID_PACKAGER_ITEM.get()))
            .displayItems((parameters, output) -> {
                // Screenshot order: left to right, then top to bottom (nine columns).
                output.accept(FLOW_METER_ITEM.get());
                output.accept(BOOSTER_PUMP_ITEM.get());
                output.accept(TURNING_BOOSTER_PUMP_ITEM.get());
                output.accept(VERTICAL_TURNING_BOOSTER_PUMP_ITEM.get());
                output.accept(BIDIRECTIONAL_FLUID_PUMP_ITEM.get());
                output.accept(REDSTONE_FLUID_PUMP_ITEM.get());
                output.accept(OMNI_PUMP_ITEM.get());
                output.accept(REDSTONE_FLUID_VALVE_ITEM.get());
                output.accept(THREE_WAY_VALVE_ITEM.get());
                output.accept(BUFFER_PUMP_ITEM.get());
                output.accept(PORTABLE_PUMP_ITEM.get());
                output.accept(RECIPROCATING_PUMP_ITEM.get());
                output.accept(FLUID_TRANSMITTER_ITEM.get());
                output.accept(CREATIVE_DELIVERY_TANK_ITEM.get());
                output.accept(FLUID_ACCESS_PORT_ITEM.get());
                output.accept(FLUID_BRIDGE_ITEM.get());
                output.accept(COPPER_WATER_SUPPLY_ITEM.get());
                output.accept(HORIZONTAL_SMART_TANK_ITEM.get());
                output.accept(FAUCET_ITEM.get());
                output.accept(SMART_FAUCET_ITEM.get());
                output.accept(FLUID_HATCH_ITEM.get());
                output.accept(SMART_FLUID_HATCH_ITEM.get());
                output.accept(COPPER_COLLECTION_GRATE_ITEM.get());
                output.accept(SMART_COLLECTION_GRATE_ITEM.get());
                output.accept(FLUID_ATOMIZER_ITEM.get());
                output.accept(COPPER_WATER_BASIN_ITEM.get());
                output.accept(SMART_FLUID_TANK_ITEM.get());
                output.accept(FLUID_ARM_ITEM.get());
                output.accept(FLUID_PACKAGER_ITEM.get());
                output.accept(FLUID_REPACKAGER_ITEM.get());
                output.accept(FLUID_GAUGE_ITEM.get());
                output.accept(FLUID_PACKAGE.get());
                for(var variant:dev.ccd.content.MetalPackages.VARIANTS) if(variant.material().equals("copper")) output.accept(variant.item().get());
                output.accept(COMPOSITE_PACKAGE.get());
                for(var variant:dev.ccd.content.MetalPackages.VARIANTS) if(variant.material().equals("brass")) output.accept(variant.item().get());
                output.accept(FLUID_FILTER.get());
                output.accept(FLUID_ATTRIBUTE_FILTER.get());
                output.accept(COPPER_BUCKET.get());
                output.accept(BRASS_BUCKET.get());
                output.accept(QUICKSAND_BUCKET.get());
                output.accept(SLIME_BUCKET.get());
                for(var medium:dev.ccd.compat.ProcessingMedia.ALL) if(medium.visible()) output.accept(medium.bucket.get());
                output.accept(net.minecraft.world.item.Items.POWDER_SNOW_BUCKET);
            }).build());
        TABS.register(bus);
        bus.addListener(this::registerCapabilities);
        bus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CCDFluids::setup);
        event.enqueueWork(dev.ccd.compat.SuperLavaFluid::setupInteractions);
        event.enqueueWork(()->BlockStressValues.IMPACTS.register(OMNI_PUMP.get(),()->BlockStressValues.getImpact(AllBlocks.MECHANICAL_PUMP.get())*CCDConfig.PUMP_STRESS_MULTIPLIER.get()));
        event.enqueueWork(() -> BlockStressValues.IMPACTS.register(BUFFER_PUMP.get(),()->BlockStressValues.getImpact(AllBlocks.MECHANICAL_PUMP.get())));
        event.enqueueWork(() -> BlockStressValues.IMPACTS.register(PORTABLE_PUMP.get(),()->BlockStressValues.getImpact(AllBlocks.MECHANICAL_PUMP.get())));
        event.enqueueWork(() -> BlockStressValues.CAPACITIES.register(PORTABLE_PUMP.get(),
            ()->BlockStressValues.getCapacity(AllBlocks.HAND_CRANK.get())+BlockStressValues.getImpact(AllBlocks.MECHANICAL_PUMP.get())));
        event.enqueueWork(() -> BlockStressValues.IMPACTS.register(BOOSTER_PUMP.get(),
            () -> BlockStressValues.getImpact(AllBlocks.MECHANICAL_PUMP.get()) * CCDConfig.PUMP_STRESS_MULTIPLIER.get()));
        event.enqueueWork(() -> BlockStressValues.IMPACTS.register(TURNING_BOOSTER_PUMP.get(),
            () -> BlockStressValues.getImpact(AllBlocks.MECHANICAL_PUMP.get()) * CCDConfig.PUMP_STRESS_MULTIPLIER.get()));
        event.enqueueWork(() -> BlockStressValues.IMPACTS.register(VERTICAL_TURNING_BOOSTER_PUMP.get(),
            () -> BlockStressValues.getImpact(AllBlocks.MECHANICAL_PUMP.get()) * CCDConfig.PUMP_STRESS_MULTIPLIER.get()));
        event.enqueueWork(() -> BlockStressValues.IMPACTS.register(REDSTONE_FLUID_PUMP.get(),
            () -> BlockStressValues.getImpact(AllBlocks.MECHANICAL_PUMP.get())));
        event.enqueueWork(() -> BlockStressValues.IMPACTS.register(BIDIRECTIONAL_FLUID_PUMP.get(),
            () -> BlockStressValues.getImpact(AllBlocks.MECHANICAL_PUMP.get())));
        event.enqueueWork(() -> BlockStressValues.IMPACTS.register(RECIPROCATING_PUMP.get(),
            () -> BlockStressValues.getImpact(AllBlocks.MECHANICAL_PUMP.get())));
        event.enqueueWork(() -> BlockStressValues.IMPACTS.register(FLUID_ARM.get(),
            () -> BlockStressValues.getImpact(AllBlocks.MECHANICAL_ARM.get())));
        event.enqueueWork(() -> BlockStressValues.IMPACTS.register(FLUID_ATOMIZER.get(),
            () -> BlockStressValues.getImpact(AllBlocks.ENCASED_FAN.get())));
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,com.simibubi.create.AllBlockEntityTypes.HEATER.get(),(be,side)->new dev.ccd.compat.SuperLavaFuel(be));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,OMNI_PUMP_BE.get(),(be,side)->be.handler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,BUFFER_PUMP_BE.get(),(be,side)->be.buffer);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,CREATIVE_DELIVERY_TANK_BE.get(),(be,side)->side==null||side==be.getBlockState().getValue(dev.ccd.content.CreativeDeliveryTankBlock.FACING)?be.handler:null);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, HORIZONTAL_SMART_TANK_BE.get(), (be,side)->be.inventory);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FLUID_ACCESS_PORT_BE.get(), dev.ccd.content.FluidAccessBlockEntity::handler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FLUID_BRIDGE_BE.get(), dev.ccd.content.FluidAccessBlockEntity::handler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FLUID_ATOMIZER_BE.get(), FluidAtomizerBlockEntity::fluidHandler);
        event.registerItem(Capabilities.FluidHandler.ITEM,
            (stack, context) -> new FluidHandlerItemStack(COPPER_BUCKET_FLUID, stack, CopperBucketItem.CAPACITY), COPPER_BUCKET.get());
        event.registerItem(Capabilities.FluidHandler.ITEM,
            (stack, context) -> new FluidHandlerItemStack(COPPER_BUCKET_FLUID, stack, CopperBucketItem.BRASS_CAPACITY), BRASS_BUCKET.get());
        // Use the native handler so frogports also recognize the direct packager path.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FLUID_PACKAGER_BE.get(),
            (be, side) -> be.inventory);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FLUID_REPACKAGER_BE.get(),
            (be, side) -> be.inventory);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, SMART_FLUID_TANK_BE.get(),
            (be, side) -> be.inventory);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, COPPER_WATER_BASIN_BE.get(),
            (be, side) -> be.getTankInventory());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, COPPER_WATER_SUPPLY_BE.get(),
            (be, side) -> be.getTankInventory());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, COPPER_COLLECTION_GRATE_BE.get(),
            (be, side) -> be.tankBehaviour().getCapability());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, SMART_COLLECTION_GRATE_BE.get(),
            (be, side) -> be.tankBehaviour().getCapability());
    }
}
