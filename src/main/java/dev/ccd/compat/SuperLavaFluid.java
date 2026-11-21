package dev.ccd.compat;

import net.minecraft.world.level.material.*;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Native lava spread, ignition, sounds, particles and dimension-dependent timing. */
public abstract class SuperLavaFluid extends LavaFluid {
    public static void setupInteractions(){
        var type=medium().type.get();
        net.neoforged.neoforge.fluids.FluidInteractionRegistry.addInteraction(type,
            new net.neoforged.neoforge.fluids.FluidInteractionRegistry.InteractionInformation(
                net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value(),state->(state.isSource()?net.minecraft.world.level.block.Blocks.OBSIDIAN:net.minecraft.world.level.block.Blocks.COBBLESTONE).defaultBlockState()));
        net.neoforged.neoforge.fluids.FluidInteractionRegistry.addInteraction(type,
            new net.neoforged.neoforge.fluids.FluidInteractionRegistry.InteractionInformation(
                (level,pos,neighbor,state)->level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.SOUL_SOIL)&&level.getBlockState(neighbor).is(net.minecraft.world.level.block.Blocks.BLUE_ICE),net.minecraft.world.level.block.Blocks.BASALT.defaultBlockState()));
    }
    private static ProcessingMedia.Medium medium(){return ProcessingMedia.ALL.getFirst();}
    @Override public Fluid getSource(){return medium().source.get();}
    @Override public Fluid getFlowing(){return medium().flowing.get();}
    @Override public net.minecraft.world.item.Item getBucket(){return medium().bucket.get();}
    @Override public net.neoforged.neoforge.fluids.FluidType getFluidType(){return medium().type.get();}
    @Override public boolean isSame(Fluid fluid){return fluid instanceof SuperLavaFluid;}
    @Override public BlockState createLegacyBlock(FluidState state){return medium().block.get().defaultBlockState().setValue(LiquidBlock.LEVEL,getLegacyLevel(state));}
    public static final class Source extends SuperLavaFluid {
        @Override public int getAmount(FluidState state){return 8;}
        @Override public boolean isSource(FluidState state){return true;}
    }
    public static final class Flowing extends SuperLavaFluid {
        @Override protected void createFluidStateDefinition(StateDefinition.Builder<Fluid,FluidState> builder){super.createFluidStateDefinition(builder);builder.add(LEVEL);}
        @Override public int getAmount(FluidState state){return state.getValue(LEVEL);}
        @Override public boolean isSource(FluidState state){return false;}
    }
}
