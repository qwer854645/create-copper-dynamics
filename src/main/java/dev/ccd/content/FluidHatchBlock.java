package dev.ccd.content;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.content.logistics.itemHatch.ItemHatchBlock;
import com.simibubi.create.content.logistics.itemHatch.ItemHatchBlockEntity;
import dev.ccd.CopperDynamics;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

/** Native hatch placement, waterlogging and wrenching; fixed fluid access without lid feedback. */
public class FluidHatchBlock extends ItemHatchBlock {
    public static final MapCodec<FluidHatchBlock> CODEC = simpleCodec(FluidHatchBlock::new);

    public FluidHatchBlock(Properties properties) { super(properties); }

    private static final net.createmod.catnip.math.VoxelShaper TROUGH_SHAPE = troughShape();
    private static net.createmod.catnip.math.VoxelShaper troughShape() {
        var shape = net.minecraft.world.phys.shapes.Shapes.or(
            box(1, 0, 1, 3, 16, 3), box(13, 0, 1, 15, 16, 3),
            box(3, 0, 1, 13, 3, 3), box(3, 13, 1, 13, 16, 3),
            box(3, 3, 0, 13, 13, 2), box(4, 4, 0, 12, 12, 4));
        for (int z = 3; z < 14; z++) {
            int top = 15 - z;
            shape = net.minecraft.world.phys.shapes.Shapes.or(shape,
                box(2, 1, z, 3, top, z + 1), box(13, 1, z, 14, top, z + 1));
        }
        // Thin staircase follows the inclined trough floor without filling its cavity.
        for (int z = 5; z < 14; z++) {
            double y = 14.65 - z;
            shape = net.minecraft.world.phys.shapes.Shapes.or(shape, box(3, y - 1.5, z, 13, y, z + 1));
        }
        return net.createmod.catnip.math.VoxelShaper.forHorizontal(shape, Direction.NORTH);
    }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
        net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return TROUGH_SHAPE.get(state.getValue(FACING));
    }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state,
        net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return getShape(state, level, pos, context);
    }


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
        if (hatch.interactionHandler(null).getTanks() == 0) {
            player.displayClientMessage(Component.translatable("ccd.fluid_hatch.no_container").withStyle(ChatFormatting.RED), true);
            return ItemInteractionResult.SUCCESS;
        }
        // Reuse the same generic container handling as Create's tank, including bottles and potions.
        if (hatch.exchangeWith(player, hand, stack)) {
            AllSoundEvents.ITEM_HATCH.playOnServer(level, pos);
        } else player.displayClientMessage(Component.translatable("ccd.fluid_hatch.rejected").withStyle(ChatFormatting.RED), true);
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
