package dev.ccd.content;

import java.util.ArrayList;
import java.util.List;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.ccd.CopperDynamics;
import dev.ccd.CCDTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;

/** Direct endpoint transfer with no fluid inventory, exposed tank or serialized fluid payload. */
public final class FluidTransmitterBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final int BATCH = 250;
    public static final int MAX_CHAIN = 64;
    private FilteringBehaviour filtering;
    private long nextTransferTick;
    private boolean transferring;

    private record Route(List<FluidTransmitterBlockEntity> nodes, IFluidHandler source, IFluidHandler target) {}

    public FluidTransmitterBlockEntity(BlockPos pos, BlockState state) {
        super(CopperDynamics.FLUID_TRANSMITTER_BE.get(), pos, state);
    }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(filtering = new FilteringBehaviour(this, new ValueBoxTransform.Sided() {
            @Override protected Vec3 getSouthLocation() { return new Vec3(.5, .5, 1); }
            @Override protected boolean isSideActive(BlockState state, Direction side) {
                return side.getAxis() != state.getValue(FluidTransmitterBlock.FACING).getAxis();
            }
        }).forFluids());
    }
    private Direction front() { return getBlockState().getValue(FluidTransmitterBlock.FACING); }
    private boolean paused() { return getBlockState().getValue(FluidTransmitterBlock.POWERED); }
    @Nullable private IFluidHandler handler(BlockPos pos, Direction side) {
        return level != null && level.hasChunkAt(pos)
            ? level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side) : null;
    }
    @Nullable private Route route() {
        Direction forward = front();
        BlockPos output = worldPosition.relative(forward);
        if (!level.hasChunkAt(output) || level.getBlockState(output).getBlock() instanceof FluidTransmitterBlock) return null;
        IFluidHandler target = handler(output, forward.getOpposite());
        if (target == null) return null;
        List<FluidTransmitterBlockEntity> nodes = new ArrayList<>();
        BlockPos cursor = worldPosition;
        for (int count = 0; count < MAX_CHAIN; count++) {
            if (!level.hasChunkAt(cursor)
                || !(level.getBlockEntity(cursor) instanceof FluidTransmitterBlockEntity node)
                || node.isRemoved() || node.front() != forward || node.paused() || node.transferring) return null;
            nodes.add(node);
            BlockPos behind = cursor.relative(forward.getOpposite());
            if (!level.hasChunkAt(behind)) return null;
            if (level.getBlockState(behind).getBlock() instanceof FluidTransmitterBlock) {
                cursor = behind;
                continue;
            }
            IFluidHandler source = handler(behind, forward);
            return source == null || source == target ? null : new Route(nodes, source, target);
        }
        return null;
    }
    @Override public void tick() {
        super.tick();
        if (level == null || level.isClientSide() || paused() || transferring || isRemoved()) return;
        BlockPos ahead = worldPosition.relative(front());
        if (level.hasChunkAt(ahead)
            && level.getBlockState(ahead).getBlock() instanceof FluidTransmitterBlock) return;
        long now = level.getGameTime();
        if (now < nextTransferTick) return;
        int interval = Math.max(1, AllConfigs.server().logistics.defaultExtractionTimer.get());
        nextTransferTick = now + interval;
        Route route = route();
        if (route == null) return;
        // Only the outlet-most node schedules a chain. Reserve every node to prevent a changed
        // topology from scheduling overlapping transfers again in this interval.
        for (var node : route.nodes()) {
            if (node != this && now < node.nextTransferTick) return;
        }
        for (var node : route.nodes()) {
            node.nextTransferTick = now + interval;
            node.transferring = true;
        }
        try {
            transfer(route);
        } finally {
            for (var node : route.nodes()) node.transferring = false;
        }
    }
    private void transfer(Route route) {
        for (int tank = 0; tank < route.source().getTanks(); tank++) {
            FluidStack sample = route.source().getFluidInTank(tank).copy();
            if (sample.isEmpty() || !route.nodes().stream().allMatch(node -> node.filtering.test(sample))) continue;
            FluidStack planned = FluidTransfers.planDrain(route.source(), sample, BATCH, false);
            if (planned.isEmpty()) continue;
            int fits = Math.clamp(route.target().fill(planned.copy(), FluidAction.SIMULATE), 0, planned.getAmount());
            if (fits == 0) continue;
            planned.setAmount(fits);
            // Check that the source still allows the exact reduced quantity before execution.
            FluidStack confirmed = route.source().drain(planned.copy(), FluidAction.SIMULATE);
            if (!FluidStack.isSameFluidSameComponents(confirmed, planned) || confirmed.getAmount() < fits) continue;
            FluidStack actual = route.source().drain(planned.copy(), FluidAction.EXECUTE);
            if (actual.isEmpty()) return;
            // Only this stack-local value owns fluid during the synchronous transfer.
            FluidStack remainder = actual;
            if (FluidStack.isSameFluidSameComponents(actual, planned) && actual.getAmount() <= fits)
                remainder = FluidTransfers.fillAndKeepRemainder(route.target(), actual);
            if (!remainder.isEmpty())
                remainder = FluidTransfers.fillAndKeepRemainder(route.source(), remainder);
            if (!remainder.isEmpty()) {
                // Never keep undelivered fluid in this device.
                Containers.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + .5,
                    worldPosition.getZ() + .5, FluidPackageItem.containingFluid(remainder));
            }
            return;
        }
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        CCDTooltip.heading(tooltip, "ccd.gui.goggles.transfer");
        CCDTooltip.line(tooltip, paused() ? ChatFormatting.YELLOW : ChatFormatting.AQUA,
            "ccd.transmitter." + (paused() ? "paused" : "ready"));
        return true;
    }
}
