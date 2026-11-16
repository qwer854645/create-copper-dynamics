package dev.ccd;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(CopperDynamics.ID)
public final class CopperDynamics {
    public static final String ID = "ccd";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ID);
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);

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
    public static final DeferredHolder<Item, FluidPackageItem> FLUID_PACKAGE =
        ITEMS.register("fluid_package", () -> new FluidPackageItem(new Item.Properties().stacksTo(1)));
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
    public static final DeferredHolder<Item, BlockItem> FLUID_PACKAGER_ITEM =
        ITEMS.register("fluid_packager", () -> new BlockItem(FLUID_PACKAGER.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidPackagerBlockEntity>> FLUID_PACKAGER_BE =
        BLOCK_ENTITIES.register("fluid_packager", () -> BlockEntityType.Builder.of(
            FluidPackagerBlockEntity::new, FLUID_PACKAGER.get()).build(null));

    public CopperDynamics(IEventBus bus) {
        ITEMS.register(bus);
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        COMPONENTS.register(bus);
        TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ccd"))
            .icon(() -> new ItemStack(FLUID_PACKAGER_ITEM.get()))
            .displayItems((parameters, output) -> {
                output.accept(FLUID_PACKAGER_ITEM.get());
                output.accept(FLUID_GAUGE_ITEM.get());
                // A valid sample for inspecting/opening the carrier in creative mode.
                var sampleItems = new net.neoforged.neoforge.items.ItemStackHandler(com.simibubi.create.content.logistics.box.PackageItem.SLOTS);
                sampleItems.setStackInSlot(0, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT));
                output.accept(CompositePackageItem.containing(sampleItems, java.util.List.of(
                    new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1_000))));
            }).build());
        TABS.register(bus);
        bus.addListener(this::registerCapabilities);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Use the native handler so frogports also recognize the direct packager path.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FLUID_PACKAGER_BE.get(),
            (be, side) -> be.inventory);
    }
}
