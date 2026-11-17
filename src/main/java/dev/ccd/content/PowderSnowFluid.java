package dev.ccd.content;

import com.simibubi.create.content.fluids.VirtualFluid;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/** Transportable snow; world placement uses the vanilla solid block, with no new flowing world liquid. */
public final class PowderSnowFluid extends VirtualFluid {
    public PowderSnowFluid(Properties properties, boolean source) { super(properties, source); }
    @Override public Item getBucket() { return Items.POWDER_SNOW_BUCKET; }
    @Override protected BlockState createLegacyBlock(FluidState state) { return Blocks.POWDER_SNOW.defaultBlockState(); }
}
