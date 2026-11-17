package dev.ccd.content;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.ComparatorUtil;
import com.simibubi.create.foundation.fluid.FluidHelper;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Independent multi-fluid storage; native tank connectivity/boiler code assumes a single fluid. */
public final class SmartFluidTankBlock extends Block implements IBE<SmartFluidTankBlockEntity>, IWrenchable {
    public static final MapCodec<SmartFluidTankBlock> CODEC = simpleCodec(SmartFluidTankBlock::new);
    public SmartFluidTankBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
        BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!FluidHatchBlock.isFluidContainer(level, stack)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide() || player instanceof FakePlayer) return ItemInteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof SmartFluidTankBlockEntity tank) {
            if (!FluidHelper.tryEmptyItemIntoBE(level, player, hand, stack, tank)) {
                FluidHelper.tryFillItemFromBE(level, player, hand, stack, tank);
            }
        }
        return ItemInteractionResult.SUCCESS;
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return getBlockEntityOptional(level, pos).map(tank -> ComparatorUtil.fractionToRedstoneLevel(
            Math.min(1f, (float) tank.inventory.amount() / tank.inventory.capacity()))).orElse(0);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        IBE.onRemove(state, level, pos, next);
        super.onRemove(state, level, pos, next, moving);
    }
    @Override public Class<SmartFluidTankBlockEntity> getBlockEntityClass() { return SmartFluidTankBlockEntity.class; }
    @Override public BlockEntityType<? extends SmartFluidTankBlockEntity> getBlockEntityType() { return CopperDynamics.SMART_FLUID_TANK_BE.get(); }
}
