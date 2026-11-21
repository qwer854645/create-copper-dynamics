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
        public final DeferredHolder<Fluid,? extends net.minecraft.world.level.material.FlowingFluid> source;
        public final DeferredHolder<Fluid,? extends net.minecraft.world.level.material.FlowingFluid> flowing;
        public final DeferredHolder<Block,LiquidBlock> block;
        public final DeferredHolder<Item,BucketItem> bucket;
        private Medium(String id,int color,int temperature) {
            this.id=id; this.color=color;
            boolean lava=id.equals("seething_reagent");
            type=TYPES.register(id,()->lava?new FluidType(FluidType.Properties.create().descriptionId("fluid_type.ccd."+id)
                .canSwim(false).canDrown(false).pathType(net.minecraft.world.level.pathfinder.PathType.LAVA).adjacentPathType(null)
                .lightLevel(15).density(3000).viscosity(6000).temperature(2000)
                .sound(SoundActions.BUCKET_FILL,SoundEvents.BUCKET_FILL_LAVA).sound(SoundActions.BUCKET_EMPTY,SoundEvents.BUCKET_EMPTY_LAVA)){
                    @Override public double motionScale(net.minecraft.world.entity.Entity entity){return net.neoforged.neoforge.common.NeoForgeMod.LAVA_TYPE.value().motionScale(entity);}
                    @Override public boolean move(net.minecraft.world.level.material.FluidState state,net.minecraft.world.entity.LivingEntity entity,net.minecraft.world.phys.Vec3 movement,double gravity){return true;}
                    @Override public void setItemMovement(net.minecraft.world.entity.item.ItemEntity entity){net.neoforged.neoforge.common.NeoForgeMod.LAVA_TYPE.value().setItemMovement(entity);}
                }:new FluidType(FluidType.Properties.create().descriptionId("fluid_type.ccd."+id)
                .density(1200).viscosity(2000).temperature(temperature)
                .sound(SoundActions.BUCKET_FILL,SoundEvents.BUCKET_FILL).sound(SoundActions.BUCKET_EMPTY,SoundEvents.BUCKET_EMPTY)));
            source=FLUIDS.register(id,()->lava?new SuperLavaFluid.Source():new BaseFlowingFluid.Source(properties()));
            flowing=FLUIDS.register("flowing_"+id,()->lava?new SuperLavaFluid.Flowing():new BaseFlowingFluid.Flowing(properties()));
            block=CopperDynamics.BLOCKS.register(id,()->new LiquidBlock(source.get(),BlockBehaviour.Properties.ofFullCopy(lava?Blocks.LAVA:Blocks.WATER)));
            bucket=CopperDynamics.ITEMS.register(id+"_bucket",()->new BucketItem(source.get(),new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
        }
        private BaseFlowingFluid.Properties properties() { return new BaseFlowingFluid.Properties(type,source,flowing).block(block).bucket(bucket).tickRate(10); }
        public boolean available() { return id.equals("seething_reagent") || net.neoforged.fml.ModList.get().isLoaded(requiredMod(id)); }
        // Temporary preview, independent of whether the optional processing recipe is available.
        public boolean visible() { return true; }
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
        new Medium("seething_reagent",0xff518fe8,1300),
        new Medium("enriching_reagent",0xff126568,300),
        new Medium("soul_stripping_reagent",0xffafeeee,300),
        new Medium("explosive_slurry",0xffa64e36,300),
        new Medium("resonant_solution",0xffb774c9,300),
        new Medium("sculk_suspension",0xff1a8b7e,300),
        new Medium("purifying_solution",0xffffedb0,300)
    );
    public static void register(IEventBus bus) { TYPES.register(bus); FLUIDS.register(bus); }
}
