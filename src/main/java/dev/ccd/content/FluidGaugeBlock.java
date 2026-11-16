package dev.ccd.content;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class FluidGaugeBlock extends FactoryPanelBlock {
    public static final MapCodec<FluidGaugeBlock> CODEC = simpleCodec(FluidGaugeBlock::new);
    public FluidGaugeBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() { return CODEC; }
    @Override public BlockEntityType<? extends FactoryPanelBlockEntity> getBlockEntityType() { return CopperDynamics.FLUID_GAUGE_BE.get(); }

    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hit) {
        if (held.is(CopperDynamics.FLUID_GAUGE_ITEM.get())) {
            if (!level.isClientSide() && LogisticallyLinkedBlockItem.networkFromStack(held) != null
                && level.getBlockEntity(pos) instanceof FluidGaugeBlockEntity gauge
                && gauge.addPanel(getTargetedSlot(pos, state, hit.getLocation()), LogisticallyLinkedBlockItem.networkFromStack(held))) {
                gauge.notifyUpdate();
                if (!player.isCreative()) held.shrink(1);
            }
            return ItemInteractionResult.SUCCESS;
        }
        if (AllBlocks.FACTORY_GAUGE.isIn(held)) return ItemInteractionResult.FAIL;
        return super.useItemOn(held, state, level, pos, player, hand, hit);
    }
    @Override public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (!context.getItemInHand().is(CopperDynamics.FLUID_GAUGE_ITEM.get())) return false;
        var be = getBlockEntity(context.getLevel(), context.getClickedPos());
        return be != null && !be.panels.get(getTargetedSlot(context.getClickedPos(), state, context.getClickLocation())).isActive();
    }
    @Override public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof FluidGaugeBlockEntity gauge)) return InteractionResult.PASS;
        var slot = getTargetedSlot(pos, state, context.getClickLocation());
        var behaviour = gauge.panels.get(slot);
        if (behaviour == null || !behaviour.isActive()) return InteractionResult.FAIL;
        if (!com.simibubi.create.Create.LOGISTICS.mayInteract(behaviour.network, player)) return InteractionResult.FAIL;
        if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled()) return InteractionResult.SUCCESS;
        if (gauge.removePanel(slot)) {
            if (!player.isCreative()) player.getInventory().placeItemBackInInventory(new ItemStack(CopperDynamics.FLUID_GAUGE_ITEM.get()));
            com.simibubi.create.content.equipment.wrench.IWrenchable.playRemoveSound(level, pos);
            if (gauge.activePanels() == 0) level.destroyBlock(pos, false);
            else gauge.notifyUpdate();
        }
        return InteractionResult.SUCCESS;
    }
    @Override public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
        return be instanceof FluidGaugeBlockEntity gauge
            ? new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, new ItemStack(CopperDynamics.FLUID_GAUGE_ITEM.get(), gauge.activePanels()))
            : ItemRequirement.NONE;
    }
}
