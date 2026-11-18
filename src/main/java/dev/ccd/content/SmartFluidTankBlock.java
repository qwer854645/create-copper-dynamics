package dev.ccd.content;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Native tank block, replacing only the entity type and multi-fluid removal transfer. */
public class SmartFluidTankBlock extends FluidTankBlock {
    public static final MapCodec<SmartFluidTankBlock> CODEC = simpleCodec(SmartFluidTankBlock::new);
    public SmartFluidTankBlock(Properties properties) { super(properties, false); }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override public BlockEntityType<? extends SmartFluidTankBlockEntity> getBlockEntityType() {
        return CopperDynamics.SMART_FLUID_TANK_BE.get();
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (next.is(this)) return;
        if (level.getBlockEntity(pos) instanceof SmartFluidTankBlockEntity tank) {
            var controller = moving || level.isClientSide() ? null : tank.getControllerBE();
            // Match native order: first remove the broken member, then split the remaining structure.
            level.removeBlockEntity(pos);
            // Moving structures already carry the controller inventory in their block-entity NBT.
            if (!moving) tank.splitAfterRemoval(controller);
            tank.destroy();
        }
    }
}
