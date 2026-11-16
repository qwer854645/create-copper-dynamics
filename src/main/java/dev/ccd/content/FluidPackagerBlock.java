package dev.ccd.content;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

public final class FluidPackagerBlock extends PackagerBlock {
    public FluidPackagerBlock(Properties properties) { super(properties); }
    @Override public BlockEntityType<? extends PackagerBlockEntity> getBlockEntityType() {
        return CopperDynamics.FLUID_PACKAGER_BE.get();
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction facing = context.getClickedFace();
        // Prefer the container against which the block was placed. Respect its exposed face.
        Direction rear = facing.getOpposite();
        if (level.hasChunkAt(pos.relative(rear)) && level.getCapability(Capabilities.FluidHandler.BLOCK,
            pos.relative(rear), facing) != null) {
            return defaultBlockState().setValue(FACING, facing).setValue(POWERED, level.hasNeighborSignal(pos));
        }
        for (Direction side : context.getNearestLookingDirections()) {
            if (level.hasChunkAt(pos.relative(side)) && level.getCapability(Capabilities.FluidHandler.BLOCK,
                pos.relative(side), side.getOpposite()) != null) {
                return defaultBlockState().setValue(FACING, side.getOpposite()).setValue(POWERED, level.hasNeighborSignal(pos));
            }
        }
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite())
            .setValue(POWERED, level.hasNeighborSignal(pos));
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
        BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(CopperDynamics.FLUID_GAUGE_ITEM.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        var handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler != null) {
            for (int i = 0; i < handler.getTanks(); i++) {
                FluidStack sample = handler.getFluidInTank(i).copy();
                if (sample.isEmpty()) continue;
                if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FluidPackagerBlockEntity be) {
                    be.setFilter(sample);
                    player.displayClientMessage(Component.translatable("ccd.packager.filter_set", sample.getHoverName()), true);
                }
                return ItemInteractionResult.SUCCESS;
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
        Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FluidPackagerBlockEntity be) {
                be.setFilter(FluidStack.EMPTY);
                player.displayClientMessage(Component.translatable("ccd.packager.filter_cleared"), true);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(ItemStack.EMPTY, state, level, pos, player, InteractionHand.MAIN_HAND, hit).consumesAction()
            ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
}
