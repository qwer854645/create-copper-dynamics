package dev.ccd.content;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.content.logistics.itemHatch.ItemHatchBlock;
import com.simibubi.create.content.logistics.itemHatch.ItemHatchBlockEntity;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Native hatch placement, geometry, waterlogging, wrenching and timed closing; fluid interaction only. */
public class FluidHatchBlock extends ItemHatchBlock {
    public static final MapCodec<FluidHatchBlock> CODEC = simpleCodec(FluidHatchBlock::new);

    public FluidHatchBlock(Properties properties) { super(properties); }

    public static boolean isFluidContainer(Level level, ItemStack stack) {
        return !stack.isEmpty() && (GenericItemEmptying.canItemBeEmptied(level, stack)
            || GenericItemFilling.canItemBeFilled(level, stack));
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
        BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Tags.Items.TOOLS_WRENCH) || !isFluidContainer(level, stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide() || player instanceof FakePlayer) return ItemInteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof FluidHatchBlockEntity hatch)) return ItemInteractionResult.SUCCESS;
        if (hatch.fluidHandler(null).getTanks() == 0) {
            player.displayClientMessage(Component.translatable("ccd.fluid_hatch.no_container"), true);
            return ItemInteractionResult.SUCCESS;
        }
        // Reuse the same generic container handling as Create's tank, including bottles and potions.
        if (hatch.exchangeWith(player, hand, stack)) {
            AllSoundEvents.ITEM_HATCH.playOnServer(level, pos);
            level.setBlockAndUpdate(pos, state.setValue(OPEN, true));
            level.scheduleTick(pos, this, 10);
        } else player.displayClientMessage(Component.translatable("ccd.fluid_hatch.rejected"), true);
        // Consume failed fluid-container interactions so a bucket cannot place its fluid into the world instead.
        return ItemInteractionResult.SUCCESS;
    }

    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (oldState.is(this) && state.getValue(FACING) != oldState.getValue(FACING)) {
            withBlockEntityDo(level, pos, ItemHatchBlockEntity::invalidateCapabilities);
        }
    }

    @Override public BlockEntityType<? extends ItemHatchBlockEntity> getBlockEntityType() {
        return CopperDynamics.FLUID_HATCH_BE.get();
    }
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
}
