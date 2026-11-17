package dev.ccd.content;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.ComparatorUtil;
import com.simibubi.create.foundation.fluid.FluidHelper;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Native tank geometry and structural search; multi-fluid contents remain distributed across members. */
public final class SmartFluidTankBlock extends Block implements IBE<SmartFluidTankBlockEntity>, IWrenchable {
    public static final MapCodec<SmartFluidTankBlock> CODEC = simpleCodec(SmartFluidTankBlock::new);
    public SmartFluidTankBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FluidTankBlock.TOP, true)
            .setValue(FluidTankBlock.BOTTOM, true).setValue(FluidTankBlock.SHAPE, FluidTankBlock.Shape.WINDOW));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FluidTankBlock.TOP, FluidTankBlock.BOTTOM, FluidTankBlock.SHAPE);
    }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide() && !old.is(this)) level.scheduleTick(pos, this, 1);
    }
    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        withBlockEntityDo(level, pos, SmartFluidTankBlockEntity::requestConnectivity);
        for (Direction direction : Direction.values()) {
            BlockPos neighbour = pos.relative(direction);
            if (level.hasChunkAt(neighbour) && level.getBlockEntity(neighbour) instanceof SmartFluidTankBlockEntity tank)
                tank.requestConnectivity();
        }
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos neighbour, boolean moving) {
        if (!level.isClientSide() && block instanceof SmartFluidTankBlock)
            withBlockEntityDo(level, pos, SmartFluidTankBlockEntity::requestConnectivity);
    }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return AllBlocks.FLUID_TANK.get().rotate(state, rotation); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return AllBlocks.FLUID_TANK.get().mirror(state, mirror); }
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
            tank.inventory.capacity() == 0 ? 0 : Math.min(1f, (float) tank.inventory.amount() / tank.inventory.capacity()))).orElse(0);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!next.is(this)) withBlockEntityDo(level, pos, SmartFluidTankBlockEntity::splitBeforeRemoval);
        IBE.onRemove(state, level, pos, next);
        super.onRemove(state, level, pos, next, moving);
    }
    @Override public Class<SmartFluidTankBlockEntity> getBlockEntityClass() { return SmartFluidTankBlockEntity.class; }
    @Override public BlockEntityType<? extends SmartFluidTankBlockEntity> getBlockEntityType() { return CopperDynamics.SMART_FLUID_TANK_BE.get(); }
}
