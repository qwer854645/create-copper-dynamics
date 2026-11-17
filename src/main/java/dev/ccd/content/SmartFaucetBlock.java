package dev.ccd.content;

import com.mojang.serialization.MapCodec;
import dev.ccd.CopperDynamics;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class SmartFaucetBlock extends FaucetBlock {
    public static final MapCodec<SmartFaucetBlock> CODEC = simpleCodec(SmartFaucetBlock::new);
    public SmartFaucetBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    @Override public BlockEntityType<? extends FaucetBlockEntity> getBlockEntityType() {
        return CopperDynamics.SMART_FAUCET_BE.get();
    }
}
