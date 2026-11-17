package dev.ccd.content;

import java.util.List;
import com.simibubi.create.foundation.fluid.FluidHelper;
import dev.ccd.CopperDynamics;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** One generic handheld tank; native item capability supplies Create filling, emptying and filter samples. */
public final class CopperBucketItem extends Item {
    public static final int CAPACITY = 4000;
    private static final int WORLD_AMOUNT = FluidType.BUCKET_VOLUME;
    public CopperBucketItem(Properties properties) { super(properties); }
    public static FluidStack contents(ItemStack stack) {
        return stack.getOrDefault(CopperDynamics.COPPER_BUCKET_FLUID.get(), SimpleFluidContent.EMPTY).copy();
    }
    @Override public Component getName(ItemStack stack) {
        FluidStack fluid = contents(stack);
        return fluid.isEmpty() ? super.getName(stack) : Component.translatable("item.ccd.copper_bucket.filled", fluid.getHoverName());
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("ccd.copper_bucket.amount", contents(stack).getAmount(), CAPACITY).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("ccd.copper_bucket.interaction").withStyle(ChatFormatting.GRAY));
    }
    @Override public boolean isBarVisible(ItemStack stack) { return !contents(stack).isEmpty(); }
    @Override public int getBarWidth(ItemStack stack) { return Math.clamp(Math.round(contents(stack).getAmount() * 13f / CAPACITY), 0, 13); }
    @Override public int getBarColor(ItemStack stack) { return 0xd88a60; }

    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        // World liquid sources are handled below without requiring a registered filled-bucket item.
        if (player == null || level.getBlockState(pos).getBlock() instanceof LiquidBlock) return InteractionResult.PASS;
        var target = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, context.getClickedFace());
        if (target == null) return InteractionResult.PASS;
        ItemStack stack = context.getItemInHand();
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        FluidActionResult result = player.isShiftKeyDown()
            ? FluidUtil.tryEmptyContainer(stack, target, CAPACITY, player, true)
            : FluidUtil.tryFillContainer(stack, target, CAPACITY, player, true);
        if (!result.isSuccess()) return InteractionResult.FAIL;
        player.setItemInHand(context.getHand(), result.getResult());
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.CONSUME;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        var handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) return InteractionResultHolder.fail(stack);
        FluidStack fluid = contents(stack);
        if (!player.isShiftKeyDown() && CAPACITY - fluid.getAmount() >= WORLD_AMOUNT) {
            BlockHitResult pickupHit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
            if (pickupHit.getType() == HitResult.Type.BLOCK && level.getFluidState(pickupHit.getBlockPos()).isSource())
                return pickUpSource(level, player, hand, stack, pickupHit);
        }
        if (fluid.getAmount() < WORLD_AMOUNT) return InteractionResultHolder.pass(stack);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
        PlacementTarget target = placementTarget(level, player, hit, fluid);
        if (target.container() || !canUse(level, player, stack, hit, target.pos())) return InteractionResultHolder.fail(stack);
        FluidStack placed = fluid.copyWithAmount(WORLD_AMOUNT);
        if (!fluid.getFluidType().canBePlacedInLevel(level, target.pos(), placed)) return InteractionResultHolder.fail(stack);
        if (level.isClientSide()) return InteractionResultHolder.sidedSuccess(stack, true);
        FluidActionResult result = FluidUtil.tryPlaceFluid(player, level, hand, target.pos(), stack, placed);
        if (!result.isSuccess()) return InteractionResultHolder.fail(stack);
        player.setItemInHand(hand, result.getResult());
        level.gameEvent(player, GameEvent.FLUID_PLACE, target.pos());
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(result.getResult(), false);
    }
    private record PlacementTarget(BlockPos pos, boolean container) {}
    private PlacementTarget placementTarget(Level level, Player player, BlockHitResult hit, FluidStack fluid) {
        BlockPos pos = hit.getBlockPos();
        var state = level.getBlockState(pos);
        // A rejected machine interaction must never turn into pouring beside that machine.
        if (!(state.getBlock() instanceof LiquidBlock) && level.getCapability(Capabilities.FluidHandler.BLOCK, pos, hit.getDirection()) != null)
            return new PlacementTarget(pos, true);
        boolean waterlog = state.getBlock() instanceof LiquidBlockContainer container
            && container.canPlaceLiquid(player, level, pos, state, fluid.getFluid());
        return new PlacementTarget(waterlog ? pos : pos.relative(hit.getDirection()), false);
    }
    private boolean canUse(Level level, Player player, ItemStack stack, BlockHitResult hit, BlockPos destination) {
        return level.mayInteract(player, hit.getBlockPos()) && level.mayInteract(player, destination)
            && player.mayUseItemAt(destination, hit.getDirection(), stack);
    }
    private InteractionResultHolder<ItemStack> pickUpSource(Level level, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        var state = level.getBlockState(pos);
        var fluidState = state.getFluidState();
        var handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        FluidStack sample = new FluidStack(FluidHelper.convertToStill(fluidState.getType()), WORLD_AMOUNT);
        if (!(state.getBlock() instanceof LiquidBlock || state.getBlock() instanceof BucketPickup)
            || !canUse(level, player, stack, hit, pos) || handler == null || handler.fill(sample, FluidAction.SIMULATE) != WORLD_AMOUNT)
            return InteractionResultHolder.fail(stack);
        if (level.isClientSide()) return InteractionResultHolder.sidedSuccess(stack, true);
        boolean removed;
        if (state.getBlock() instanceof LiquidBlock) {
            // BucketPickupHandlerWrapper expects a BucketItem result; unbucketed fluids have none.
            removed = level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        } else {
            ItemStack vanillaResult = ((BucketPickup) state.getBlock()).pickupBlock(player, level, pos, state);
            removed = !vanillaResult.isEmpty() || !level.getFluidState(pos).equals(fluidState);
        }
        if (!removed) return InteractionResultHolder.fail(stack);
        handler.fill(sample, FluidAction.EXECUTE);
        player.setItemInHand(hand, handler.getContainer());
        level.playSound(null, pos, FluidHelper.getFillSound(sample), SoundSource.BLOCKS, 1f, 1f);
        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(handler.getContainer(), false);
    }
}
