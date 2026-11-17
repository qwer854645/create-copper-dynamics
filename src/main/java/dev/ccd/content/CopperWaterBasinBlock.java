package dev.ccd.content;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.foundation.fluid.FluidHelper;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Native tank block with a fixed water source and fill-only handheld interaction. */
public final class CopperWaterBasinBlock extends FluidTankBlock {
    public static final MapCodec<CopperWaterBasinBlock> CODEC = simpleCodec(CopperWaterBasinBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 4, 16),
        Block.box(0, 4, 0, 16, 14, 1), Block.box(0, 4, 15, 16, 14, 16),
        Block.box(0, 4, 1, 1, 14, 15), Block.box(15, 4, 1, 16, 14, 15));

    public CopperWaterBasinBlock(Properties properties) { super(properties, true); }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
        BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof CopperWaterBasinBlockEntity basin)) return ItemInteractionResult.FAIL;
        // Always fill first; the native tank's empty-first interaction would discard a partly filled container.
        if (FluidHelper.tryFillItemFromBE(level, player, hand, stack, basin)) {
            if (!level.isClientSide()) level.playSound(null, pos,
                FluidHelper.getFillSound(basin.getTankInventory().getFluid()), SoundSource.BLOCKS, .5f, 1f);
            return ItemInteractionResult.SUCCESS;
        }
        // A full/incompatible fluid container must not fall through to placing or pouring its contents.
        if (GenericItemFilling.canItemBeFilled(level, stack) || GenericItemEmptying.canItemBeEmptied(level, stack))
            return ItemInteractionResult.SUCCESS;
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    @Override public BlockEntityType<? extends CopperWaterBasinBlockEntity> getBlockEntityType() {
        return CopperDynamics.COPPER_WATER_BASIN_BE.get();
    }
}
