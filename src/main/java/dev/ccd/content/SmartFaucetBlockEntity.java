package dev.ccd.content;

import java.lang.ref.WeakReference;
import java.util.List;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import dev.ccd.CopperDynamics;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;

/** Native recipe and belt APIs, supplied directly from the rear container at completion. */
public final class SmartFaucetBlockEntity extends FaucetBlockEntity {
    private FilteringBehaviour filtering;
    private WeakReference<TransportedItemStack> processing = new WeakReference<>(null);
    private ItemStack inputIdentity = ItemStack.EMPTY;
    private long readyAt, lastSeen;
    private record FillPlan(IFluidHandler source, FluidStack offered, int required) {}
    private record Scan(@Nullable FillPlan plan, boolean waitForFluid) {}

    public SmartFaucetBlockEntity(BlockPos pos, BlockState state) {
        super(CopperDynamics.SMART_FAUCET_BE.get(), pos, state);
    }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.add(filtering = new FilteringBehaviour(this, new ValueBoxTransform.Sided() {
            @Override protected Vec3 getSouthLocation() { return new Vec3(.5, .5, .65); }
            @Override protected boolean isSideActive(BlockState state, Direction side) {
                return side != Direction.DOWN && side != state.getValue(FaucetBlock.FACING).getOpposite();
            }
        }).forFluids());
        behaviours.add(new BeltProcessingBehaviour(this).whenItemEnters(this::receive).whileItemHeld(this::hold));
    }
    @Override protected boolean acceptsFluid(FluidStack fluid) { return filtering.test(fluid); }
    @Override protected boolean allowContainerTransfer() { return processing.get() == null; }
    @Override public void tick() {
        if (level != null && (!enabled() || level.getGameTime() - lastSeen > 1)) resetProcessing();
        super.tick();
    }
    private void resetProcessing() {
        processing.clear();
        inputIdentity = ItemStack.EMPTY;
        readyAt = 0;
    }
    private boolean canProcess(TransportedItemStack item, TransportedItemStackHandlerBehaviour handler) {
        return level != null && !level.isClientSide() && !isRemoved() && !handler.blockEntity.isVirtual()
            && enabled() && !transferring && !BeltProcessingBehaviour.isBlocked(level, worldPosition.below(2))
            && FillingBySpout.canItemBeFilled(level, item.stack);
    }
    private ProcessingResult receive(TransportedItemStack item, TransportedItemStackHandlerBehaviour handler) {
        if (!canProcess(item, handler)) return ProcessingResult.PASS;
        Scan scan = scan(item.stack);
        return scan.plan() != null || scan.waitForFluid() ? ProcessingResult.HOLD : ProcessingResult.PASS;
    }
    private Scan scan(ItemStack item) {
        IFluidHandler source = inputHandler();
        if (source == null) return new Scan(null, true);
        boolean sawFluid = false, needsMore = false;
        for (int tank = 0; tank < source.getTanks(); tank++) {
            FluidStack sample = source.getFluidInTank(tank).copy();
            if (sample.isEmpty() || !acceptsFluid(sample)) continue;
            FluidStack offered = FluidTransfers.planDrain(source, sample, LIMIT, false);
            if (offered.isEmpty()) continue;
            sawFluid = true;
            int required = FillingBySpout.getRequiredAmountForItem(level, item.copy(), offered.copy());
            if (required <= 0 || required > LIMIT) continue;
            if (required > offered.getAmount()) { needsMore = true; continue; }
            return new Scan(new FillPlan(source, offered, required), false);
        }
        return new Scan(null, needsMore || !sawFluid);
    }
    private ProcessingResult hold(TransportedItemStack item, TransportedItemStackHandlerBehaviour handler) {
        if (!canProcess(item, handler)) { resetProcessing(); return ProcessingResult.PASS; }
        lastSeen = level.getGameTime();
        Scan scan = scan(item.stack);
        if (scan.plan() == null) {
            resetProcessing();
            return scan.waitForFluid() ? ProcessingResult.HOLD : ProcessingResult.PASS;
        }
        if (processing.get() != item || !ItemStack.isSameItemSameComponents(inputIdentity, item.stack)) {
            processing = new WeakReference<>(item);
            inputIdentity = item.stack.copyWithCount(1);
            readyAt = lastSeen + SpoutBlockEntity.FILLING_TIME;
            AllSoundEvents.SPOUTING.playOnServer(level, worldPosition, .75f, 1);
            return ProcessingResult.HOLD;
        }
        if (lastSeen < readyAt) return ProcessingResult.HOLD;
        transferring = true;
        try {
            complete(item, handler, scan.plan());
        } finally {
            transferring = false;
            resetProcessing();
        }
        return ProcessingResult.HOLD;
    }
    private void complete(TransportedItemStack item, TransportedItemStackHandlerBehaviour handler, FillPlan plan) {
        // Evaluate the native operation on copies. No physical item changes before exact withdrawal succeeds.
        ItemStack original = item.stack.copy();
        ItemStack leftover = original.copy();
        FluidStack fluid = plan.offered().copy();
        ItemStack result = FillingBySpout.fillItem(level, plan.required(), leftover, fluid);
        if (leftover.getCount() != item.stack.getCount() - 1
            || plan.offered().getAmount() - fluid.getAmount() != plan.required()) return;
        FluidStack request = plan.offered().copyWithAmount(plan.required());
        FluidStack confirmed = plan.source().drain(request.copy(), FluidAction.SIMULATE);
        if (!FluidStack.isSameFluidSameComponents(confirmed, request) || confirmed.getAmount() < request.getAmount()) return;
        FluidStack actual = plan.source().drain(request.copy(), FluidAction.EXECUTE);
        if (!FluidStack.matches(actual, request)) {
            if (!actual.isEmpty()) returnUndelivered(plan.source(), actual);
            return;
        }
        if (!enabled() || !acceptsFluid(actual) || isRemoved()
            || BeltProcessingBehaviour.isBlocked(level, worldPosition.below(2)) || handler.blockEntity.isRemoved()
            || level.getBlockEntity(handler.getPos()) != handler.blockEntity) {
            returnUndelivered(plan.source(), actual);
            return;
        }
        boolean[] committed = { false };
        handler.handleProcessingOnAllItems(current -> {
            if (committed[0] || current != item || !ItemStack.matches(current.stack, original)) return null;
            current.clearFanProcessingData();
            var output = current.copy();
            output.stack = result;
            var remaining = current.copy();
            remaining.stack = leftover;
            committed[0] = true;
            return TransportedResult.convertToAndLeaveHeld(result.isEmpty() ? List.of() : List.of(output),
                leftover.isEmpty() ? null : remaining);
        });
        if (committed[0]) splash(actual, 2);
        else returnUndelivered(plan.source(), actual);
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        super.addToGoggleTooltip(tooltip, sneaking);
        tooltip.add(Component.translatable("ccd.smart_faucet.items").withStyle(ChatFormatting.GRAY));
        return true;
    }
}
