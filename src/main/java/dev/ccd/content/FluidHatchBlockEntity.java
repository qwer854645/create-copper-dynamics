package dev.ccd.content;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.logistics.itemHatch.ItemHatchBlockEntity;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.FluidHelper;
import dev.ccd.CopperDynamics;
import dev.ccd.CCDTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/** No storage: each operation resolves the current sided capability behind the native hatch. */
public class FluidHatchBlockEntity extends ItemHatchBlockEntity implements IHaveGoggleInformation {
    private static final ThreadLocal<Set<FluidHatchBlockEntity>> ACTIVE = ThreadLocal.withInitial(HashSet::new);
    private final IFluidHandler unsided = new Proxy(null);
    private final IFluidHandler[] sides = new IFluidHandler[Direction.values().length];
    @Nullable private ItemStack exchangeContainer;

    public FluidHatchBlockEntity(BlockPos pos, BlockState state) {
        this(CopperDynamics.FLUID_HATCH_BE.get(), pos, state);
    }
    protected FluidHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (Direction direction : Direction.values()) sides[direction.get3DDataValue()] = new Proxy(direction);
    }

    // The basic hatch has neither the native item filter nor an item inventory.
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}
    @Override public void clearContent() {}
    protected boolean acceptsFluid(FluidStack fluid) { return true; }
    protected FluidStack drainAllowed(IFluidHandler target, int amount, IFluidHandler.FluidAction action) {
        return target.drain(amount, action);
    }

    public boolean exchangeWith(Player player, InteractionHand hand, ItemStack stack) {
        if (level == null || level.isClientSide() || exchangeContainer != null) return false;
        exchangeContainer = stack.copy();
        try {
            return FluidHelper.tryEmptyItemIntoBE(level, player, hand, stack, this)
                || FluidHelper.tryFillItemFromBE(level, player, hand, stack, this);
        } finally {
            exchangeContainer = null;
        }
    }

    private boolean accessible(@Nullable Direction side) {
        return side == null || side == getBlockState().getValue(FluidHatchBlock.FACING).getOpposite();
    }
    /** Explicit handheld/arm access only; deliberately not registered as a world fluid capability. */
    @Nullable public IFluidHandler interactionHandler(@Nullable Direction side) {
        return !accessible(side) ? null : side == null ? unsided : sides[side.get3DDataValue()];
    }

    private <T> T access(@Nullable Direction side, Function<IFluidHandler, T> operation, T unavailable) {
        if (level == null || isRemoved() || !accessible(side) || !level.hasChunkAt(worldPosition)
            || level.getBlockEntity(worldPosition) != this) return unavailable;
        Set<FluidHatchBlockEntity> active = ACTIVE.get();
        // Also handles loops through another mod's forwarding capability; never force-load the target chunk.
        if (active.size() >= 64 || !active.add(this)) return unavailable;
        try {
            Direction towardContainer = getBlockState().getValue(FluidHatchBlock.FACING);
            BlockPos targetPos = worldPosition.relative(towardContainer);
            if (!level.hasChunkAt(targetPos)) return unavailable;
            IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK,
                targetPos, towardContainer.getOpposite());
            return target == null ? unavailable : operation.apply(target);
        } finally {
            active.remove(this);
            if (active.isEmpty()) ACTIVE.remove();
        }
    }

    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        boolean connected = containedFluidTooltip(tooltip, sneaking, unsided);
        if (!connected) {
            CCDTooltip.heading(tooltip, "ccd.gui.goggles.connection");
            CCDTooltip.line(tooltip, ChatFormatting.YELLOW, "ccd.fluid_hatch.no_container");
        }
        return true;
    }

    private final class Proxy implements IFluidHandler {
        @Nullable private final Direction side;
        private Proxy(@Nullable Direction side) { this.side = side; }
        @Override public int getTanks() { return access(side, IFluidHandler::getTanks, 0); }
        @Override public FluidStack getFluidInTank(int tank) {
            return access(side, target -> {
                if (tank < 0 || tank >= target.getTanks()) return FluidStack.EMPTY;
                FluidStack sample = target.getFluidInTank(tank).copy();
                if (exchangeContainer == null || side != null || sample.isEmpty()) return sample;
                if (!acceptsFluid(sample)) return FluidStack.EMPTY;
                // Native tryFillItemFromBE checks the displayed amount, not drain(SIMULATE).
                // Require this container's exact amount, respecting read-only, rate-limited and discrete sources.
                int required = GenericItemFilling.getRequiredAmountForItem(level, exchangeContainer.copy(), sample.copy());
                if (required <= 0 || required > sample.getAmount()) return FluidStack.EMPTY;
                FluidStack drainable = target.drain(sample.copyWithAmount(required), FluidAction.SIMULATE);
                return FluidStack.isSameFluidSameComponents(sample, drainable) && drainable.getAmount() >= required
                    ? sample : FluidStack.EMPTY;
            }, FluidStack.EMPTY);
        }
        @Override public int getTankCapacity(int tank) {
            return access(side, target -> tank >= 0 && tank < target.getTanks() ? target.getTankCapacity(tank) : 0, 0);
        }
        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return !stack.isEmpty() && acceptsFluid(stack) && access(side,
                target -> tank >= 0 && tank < target.getTanks() && target.isFluidValid(tank, stack.copy()), false);
        }
        @Override public int fill(FluidStack resource, FluidAction action) {
            return resource.isEmpty() || !acceptsFluid(resource) ? 0 : access(side, target -> target.fill(resource.copy(), action), 0);
        }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            return resource.isEmpty() || !acceptsFluid(resource) ? FluidStack.EMPTY
                : access(side, target -> target.drain(resource.copy(), action), FluidStack.EMPTY);
        }
        @Override public FluidStack drain(int amount, FluidAction action) {
            return amount <= 0 ? FluidStack.EMPTY : access(side, target -> drainAllowed(target, amount, action), FluidStack.EMPTY);
        }
    }
}
