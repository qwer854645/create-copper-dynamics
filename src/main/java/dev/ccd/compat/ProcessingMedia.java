package dev.ccd.compat;

import dev.ccd.CopperDynamics;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Stable CCD reagent identities, independent of optional mod installation order. */
public final class ProcessingMedia {
    private static final DeferredRegister<FluidType> TYPES=DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES,"ccd");
    private static final DeferredRegister<Fluid> FLUIDS=DeferredRegister.create(Registries.FLUID,"ccd");
    public static final class Medium {
        public final String id;
        public final int color;
        public final DeferredHolder<FluidType,FluidType> type;
        public final DeferredHolder<Fluid,BaseFlowingFluid.Source> source;
        public final DeferredHolder<Fluid,BaseFlowingFluid.Flowing> flowing;
        public final DeferredHolder<Block,LiquidBlock> block;
        public final DeferredHolder<Item,BucketItem> bucket;
        private Medium(String id,int color,int temperature) {
            this.id=id; this.color=color;
            type=TYPES.register(id,()->new FluidType(FluidType.Properties.create().descriptionId("fluid_type.ccd."+id)
                .density(1200).viscosity(2000).temperature(temperature)
                .sound(SoundActions.BUCKET_FILL,SoundEvents.BUCKET_FILL).sound(SoundActions.BUCKET_EMPTY,SoundEvents.BUCKET_EMPTY)));
            source=FLUIDS.register(id,()->new BaseFlowingFluid.Source(properties()));
            flowing=FLUIDS.register("flowing_"+id,()->new BaseFlowingFluid.Flowing(properties()));
            block=CopperDynamics.BLOCKS.register(id,()->new LiquidBlock(source.get(),BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)));
            bucket=CopperDynamics.ITEMS.register(id+"_bucket",()->new BucketItem(source.get(),new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
        }
        private BaseFlowingFluid.Properties properties() { return new BaseFlowingFluid.Properties(type,source,flowing).block(block).bucket(bucket).tickRate(10); }
        public boolean available() { return net.neoforged.fml.ModList.get().isLoaded(requiredMod(id)); }
    }
    public static String requiredMod(String id) {
        return switch(id) {
            case "seething_reagent" -> "dndesires";
            case "enriching_reagent" -> "createnuclear";
            case "soul_stripping_reagent" -> "createnetherindustry";
            default -> "create_more_catalysts";
        };
    }
    public static final List<Medium> ALL=List.of(
        new Medium("seething_reagent",0xff74b9e8,1300),
        new Medium("enriching_reagent",0xff85b741,300),
        new Medium("soul_stripping_reagent",0xff58c6ca,300),
        new Medium("explosive_slurry",0xffb98a55,300),
        new Medium("resonant_solution",0xffa682cb,300),
        new Medium("sculk_suspension",0xff226b70,300),
        new Medium("purifying_solution",0xffc8ecdc,300)
    );
    public static void register(IEventBus bus) { TYPES.register(bus); FLUIDS.register(bus); }
}
