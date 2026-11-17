package dev.ccd.content;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.logistics.itemHatch.ItemHatchBlockEntity;
import dev.ccd.CopperDynamics;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class SmartFluidHatchBlock extends FluidHatchBlock {
    public static final MapCodec<SmartFluidHatchBlock> CODEC = simpleCodec(SmartFluidHatchBlock::new);
    public SmartFluidHatchBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    @Override public BlockEntityType<? extends ItemHatchBlockEntity> getBlockEntityType() {
        return CopperDynamics.SMART_FLUID_HATCH_BE.get();
    }
}
