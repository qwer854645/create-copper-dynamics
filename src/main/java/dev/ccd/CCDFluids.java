package dev.ccd;

import com.simibubi.create.api.behaviour.spouting.CauldronSpoutingBehavior;
import dev.ccd.content.PowderSnowFluid;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.CauldronFluidContent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.RegisterCauldronFluidContentEvent;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@EventBusSubscriber(modid = CopperDynamics.ID, bus = EventBusSubscriber.Bus.MOD)
public final class CCDFluids {
    private CCDFluids() {}
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, CopperDynamics.ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, CopperDynamics.ID);
    public static final DeferredHolder<FluidType, FluidType> QUICKSAND_TYPE = TYPES.register("quicksand", () -> new FluidType(
        FluidType.Properties.create().descriptionId("fluid_type.ccd.quicksand").density(1800).viscosity(3000)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL).sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> QUICKSAND = FLUIDS.register("quicksand",
        () -> new BaseFlowingFluid.Source(quicksandProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_QUICKSAND = FLUIDS.register("flowing_quicksand",
        () -> new BaseFlowingFluid.Flowing(quicksandProperties()));
    private static BaseFlowingFluid.Properties quicksandProperties() {
        return new BaseFlowingFluid.Properties(QUICKSAND_TYPE, QUICKSAND, FLOWING_QUICKSAND)
            .block(CopperDynamics.QUICKSAND_BLOCK).bucket(CopperDynamics.QUICKSAND_BUCKET).tickRate(15).levelDecreasePerBlock(2);
    }
    public static final DeferredHolder<FluidType, FluidType> POWDER_SNOW_TYPE = TYPES.register("powder_snow", () -> new FluidType(
        FluidType.Properties.create().descriptionId("fluid_type.ccd.powder_snow").temperature(263).density(500).viscosity(2000)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_POWDER_SNOW)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_POWDER_SNOW)));
    public static final DeferredHolder<Fluid, PowderSnowFluid> POWDER_SNOW = FLUIDS.register("powder_snow",
        () -> new PowderSnowFluid(properties(), true));
    public static final DeferredHolder<Fluid, PowderSnowFluid> FLOWING_POWDER_SNOW = FLUIDS.register("flowing_powder_snow",
        () -> new PowderSnowFluid(properties(), false));
    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(POWDER_SNOW_TYPE, POWDER_SNOW, FLOWING_POWDER_SNOW);
    }
    public static void register(IEventBus bus) { TYPES.register(bus); FLUIDS.register(bus); }
    public static void setup() {
        // A full cauldron and vanilla bucket both represent exactly 1000 mB; no rounded one-third spout recipe.
        CauldronSpoutingBehavior.CAULDRON_INFO.register(POWDER_SNOW.get(), new CauldronSpoutingBehavior.CauldronInfo(
            FluidType.BUCKET_VOLUME, Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3)));
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void cauldrons(RegisterCauldronFluidContentEvent event) {
        // Do not duplicate a registration if another mod has already attached a fluid to the vanilla snow cauldron.
        if (CauldronFluidContent.getForBlock(Blocks.POWDER_SNOW_CAULDRON) == null)
            event.register(Blocks.POWDER_SNOW_CAULDRON, POWDER_SNOW.get(), FluidType.BUCKET_VOLUME, LayeredCauldronBlock.LEVEL);
    }
    @SubscribeEvent public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new SnowBucketWrapper(stack), Items.POWDER_SNOW_BUCKET);
    }
    /** The snow bucket is a SolidBucketItem, so the normal BucketItem wrapper cannot read its contents. */
    private static final class SnowBucketWrapper extends FluidBucketWrapper {
        private SnowBucketWrapper(ItemStack stack) { super(stack); }
        @Override public FluidStack getFluid() {
            return container.is(Items.POWDER_SNOW_BUCKET) ? new FluidStack(POWDER_SNOW.get(), FluidType.BUCKET_VOLUME) : super.getFluid();
        }
    }
}
