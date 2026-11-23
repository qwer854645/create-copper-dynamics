package dev.ccd.content;

import java.util.List;
import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import dev.ccd.logistics.FluidInventoryBehaviour;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.compat.computercraft.events.PackageEvent;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ccd.CopperDynamics;
import dev.ccd.CCDTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;

public final class FluidPackagerBlockEntity extends PackagerBlockEntity implements IHaveGoggleInformation {
    private FluidStack filter = FluidStack.EMPTY;
    private FluidStack recovery = FluidStack.EMPTY;
    private ItemStack compositeRecovery = ItemStack.EMPTY;
    private String status = "ready";
    private InventorySummary fluidSummary;
    private final dev.ccd.logistics.AdjacentFluidCapability fluidEndpoint=new dev.ccd.logistics.AdjacentFluidCapability(this);

    public FluidPackagerBlockEntity(BlockPos pos, BlockState state) {
        super(CopperDynamics.FLUID_PACKAGER_BE.get(), pos, state);
    }

    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.removeIf(behaviour -> behaviour instanceof InvManipulationBehaviour);
        behaviours.add(targetInventory = new FluidInventoryBehaviour(this));
    }

    @Override public InventorySummary getAvailableItems() {
        var scoped = dev.ccd.logistics.FluidStockScope.summarize(this);
        if (scoped != null) return scoped;
        InventorySummary next = new InventorySummary();
        var sources = level == null ? java.util.List.<dev.ccd.logistics.FluidAccess.Source>of()
            : dev.ccd.logistics.FluidAccess.resolve(level, targetFluidPos(), getBlockState().getValue(PackagerBlock.FACING));
        for (var source : sources) for (int tank = 0, tanks = source.handler().getTanks(); tank < tanks; tank++) {
            FluidStack fluid = source.handler().getFluidInTank(tank).copy();
            if (fluid.isEmpty() || !source.filter().test(fluid)) continue;
            ItemStack identity = FluidStockItem.of(fluid);
            int existing = next.getCountOf(identity);
            next.add(identity, Math.max(0, Math.min(fluid.getAmount(), BigItemStack.INF - existing)));
        }
        if (fluidSummary != null && level != null && !level.isClientSide()) notifyFluidArrivals(fluidSummary, next);
        fluidSummary = next;
        return next;
    }
    @Override public void triggerStockCheck() {
        getAvailableItems();
        if (level == null || level.isClientSide()) return;
        for (Direction side : Direction.values()) {
            BlockPos pos = worldPosition.relative(side);
            if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof PackagerLinkBlockEntity link
                && link.getPackager() == this) {
                LogisticsManager.SUMMARIES.invalidate(link.behaviour.freqId);
                LogisticsManager.ACCURATE_SUMMARIES.invalidate(link.behaviour.freqId);
            }
        }
    }
    private void notifyFluidArrivals(InventorySummary before, InventorySummary after) {
        for (BigItemStack entry : after.getStacks()) {
            int added = entry.count - before.getCountOf(entry.stack);
            if (added <= 0) continue;
            for (Direction side : Direction.values()) {
                BlockPos pos = worldPosition.relative(side);
                if (!level.hasChunkAt(pos)) continue;
                if (level.getBlockEntity(pos) instanceof FluidGaugeBlockEntity gauge && gauge.getRestockedPackager() == this)
                    for (var panel : gauge.panels.values()) panel.restockerPromises.itemEnteredSystem(entry.stack, added);
                if (level.getBlockEntity(pos) instanceof PackagerLinkBlockEntity link && link.getPackager() == this) {
                    var promises = Create.LOGISTICS.getQueuedPromises(link.behaviour.freqId);
                    if (promises != null) promises.itemEnteredSystem(entry.stack, added);
                }
            }
        }
    }
    @Override public boolean isTargetingSameInventory(@Nullable IdentifiedInventory other) {
        var own = targetInventory.getIdentifiedInventory();
        if (other != null && own != null
            && own.identifier() instanceof FluidInventoryBehaviour.AccessId a
            && other.identifier() instanceof FluidInventoryBehaviour.AccessId b)
            return a.dimension().equals(b.dimension()) && !a.sources().isEmpty() && a.sources().equals(b.sources());
        return other != null && own != null && own.identifier() != null && own.identifier().equals(other.identifier());
    }

    @Override public void tick() {
        super.tick();
        if (level != null && !level.isClientSide() && heldBox.isEmpty() && animationTicks == 0
            && !hasRecovery() && status.equals("output")) status("ready");
    }

    public void setFilter(FluidStack sample) {
        filter = sample.isEmpty() ? FluidStack.EMPTY : sample.copyWithAmount(1);
        triggerStockCheck();
        notifyUpdate();
    }
    public FluidStack getFilter() { return filter.copy(); }
    public FluidStack getRecovery() { return recovery.copy(); }
    public ItemStack getCompositeRecovery() { return compositeRecovery.copy(); }
    private boolean hasRecovery() { return !recovery.isEmpty() || !compositeRecovery.isEmpty(); }
    public String getStatus() { return status; }
    private void status(String next) {
        if (next.equals(status)) return;
        status = next;
        notifyUpdate();
    }

    @Nullable public IFluidHandler targetFluidHandler() {
        if (level == null) return null;
        Direction facing = getBlockState().getValue(PackagerBlock.FACING);
        BlockPos target = worldPosition.relative(facing.getOpposite());
        if (!level.hasChunkAt(target)) return null;
        return fluidEndpoint.get(target,facing);
    }
    public BlockPos targetFluidPos() {
        return worldPosition.relative(getBlockState().getValue(PackagerBlock.FACING).getOpposite());
    }

    @Override public void lazyTick() {
        if (level == null || level.isClientSide()) return;
        if (!compositeRecovery.isEmpty()) {
            ItemStack before=compositeRecovery.copy();
            compositeRecovery = deliverComposite(compositeRecovery, false);
            status(hasRecovery() ? "recovery" : "ready");
            if(!ItemStack.matches(before,compositeRecovery))notifyUpdate();
            return;
        }
        if (!recovery.isEmpty()) {
            IFluidHandler target = targetFluidHandler();
            if (target != null) {
                int before = recovery.getAmount();
                recovery = FluidTransfers.fillAndKeepRemainder(target, recovery);
                if (recovery.getAmount() != before) notifyUpdate();
            }
            status(recovery.isEmpty() ? "ready" : "recovery");
            return;
        }
        super.lazyTick();
    }

    @Override public void attemptToSend(@Nullable List<PackagingRequest> requests) {
        if (level == null || level.isClientSide()) return;
        if (requests != null) { packageNativeRequest(requests); return; }
        if (!heldBox.isEmpty() || animationTicks != 0 || buttonCooldown > 0) return;
        if (hasRecovery()) { status("recovery"); return; }
        IFluidHandler source = targetFluidHandler();
        if (source == null) { status("no_container"); return; }
        FluidStack planned = FluidTransfers.planDrain(source, filter, FluidPackageItem.CAPACITY, false);
        if (planned.isEmpty()) { status("no_fluid"); return; }
        FluidStack actual = source.drain(planned.copy(), FluidAction.EXECUTE);
        if (actual.isEmpty()) { status("no_fluid"); return; }
        // Keep even an unexpected actual result: external simulation is not a transaction.
        if (actual.getAmount() > FluidPackageItem.CAPACITY
            || !FluidStack.isSameFluidSameComponents(actual, planned)) {
            recovery = actual.copy();
            status("recovery");
            notifyUpdate();
            return;
        }
        ItemStack box = FluidPackageItem.containingFluid(actual);
        updateSignAddress();
        if (!signBasedAddress.isBlank()) PackageItem.addAddress(box, signBasedAddress);
        heldBox = box;
        animationInward = false;
        animationTicks = CYCLE;
        computerBehaviour.prepareComputerEvent(new PackageEvent(box, "package_created"));
        status("output");
        notifyUpdate();
    }

    /** Native LogisticsManager invokes this repeatedly and owns the request counters.
     * Withdraw now and reuse its serialized outgoing-package queue, so pending cargo
     * cannot also remain advertised as free fluid stock. */
    private void packageNativeRequest(List<PackagingRequest> requests) {
        if (requests.isEmpty() || hasRecovery()) return;
        PackagingRequest request = requests.getFirst();
        FluidStack fluid = FluidStockItem.fluid(request.item());
        var source = targetFluidHandler();
        if (source == null || fluid.isEmpty() || request.getCount() <= 0) { requests.removeFirst(); return; }
        int limit = Math.min(FluidPackageItem.CAPACITY, request.getCount());
        FluidStack planned = FluidTransfers.planDrain(source, fluid, limit, false);
        if (planned.isEmpty()) { requests.removeFirst(); return; }
        FluidStack actual = source.drain(planned.copy(), FluidAction.EXECUTE);
        if (actual.isEmpty()) { requests.removeFirst(); return; }
        if (!FluidStack.isSameFluidSameComponents(actual, fluid) || actual.getAmount() > limit) {
            recovery = actual.copy(); status("recovery"); notifyUpdate(); return;
        }
        request.subtract(actual.getAmount());
        int fragment = request.packageCounter().getAndIncrement();
        boolean finalFragment = request.isEmpty();
        if (finalFragment) requests.removeFirst();
        if (!requests.isEmpty() && requests.getFirst().orderId() == request.orderId()
            && requests.getFirst().linkIndex() == request.linkIndex()) {
            finalFragment = false;
            // All fluid kinds from one source link share one fragment sequence.
            // Native processRequest creates a counter per requested kind; continue
            // its value when advancing to the next kind on the same order/link.
            if (request.isEmpty()) requests.getFirst().packageCounter().setValue(
                Math.max(request.packageCounter().intValue(), requests.getFirst().packageCounter().intValue()));
        }
        ItemStack box = FluidPackageItem.containingFluid(actual);
        PackageItem.addAddress(box, request.address());
        PackageItem.setOrder(box, request.orderId(), request.linkIndex(), request.finalLink().booleanValue(),
            fragment, finalFragment, request.context());
        if (heldBox.isEmpty() && animationTicks == 0) {
            heldBox = box; animationInward = false; animationTicks = CYCLE;
        } else queuedExitingPackages.add(new BigItemStack(box, 1));
        computerBehaviour.prepareComputerEvent(new PackageEvent(box, "package_created"));
        triggerStockCheck();
        status("output");
        notifyUpdate();
    }

    @Override public boolean unwrapBox(ItemStack box, boolean simulate) {
        if (level == null || level.isClientSide() || animationTicks != 0 || !heldBox.isEmpty()
            || hasRecovery()) return false;
        if (CompositePackageItem.isComposite(box)) {
            if (!CompositePackageItem.isValid(box)) return false;
            if (simulate) return deliverComposite(box, true).isEmpty();
            compositeRecovery = deliverComposite(box, false);
            previouslyUnwrapped = box.copyWithCount(1);
            animationInward = true;
            animationTicks = CYCLE;
            computerBehaviour.prepareComputerEvent(new PackageEvent(box, "package_received"));
            status(hasRecovery() ? "recovery" : "ready");
            notifyUpdate();
            return true;
        }
        if (!FluidPackageItem.isValid(box)) return false;
        FluidStack fluid = FluidPackageItem.contents(box);
        IFluidHandler target = targetFluidHandler();
        if (simulate) return target != null && FluidTransfers.canFillAll(target, fluid);
        // Native callers simulate first and may ignore the second call's return value.
        // Once execution begins, take ownership of the complete payload even if the
        // capability disappeared or changed after the successful simulation.
        recovery = target == null ? fluid.copy() : FluidTransfers.fillAndKeepRemainder(target, fluid);
        previouslyUnwrapped = box.copyWithCount(1);
        animationInward = true;
        animationTicks = CYCLE;
        computerBehaviour.prepareComputerEvent(new PackageEvent(box, "package_received"));
        status(recovery.isEmpty() ? "ready" : "recovery");
        notifyUpdate();
        return true;
    }

    /** Execute takes ownership of every remainder; generic capabilities cannot reserve space. */
    private ItemStack deliverComposite(ItemStack box, boolean simulate) {
        var remainingItems = new net.neoforged.neoforge.items.ItemStackHandler(PackageItem.SLOTS);
        var items = PackageItem.getContents(box);
        Direction facing = getBlockState().getValue(PackagerBlock.FACING);
        var targetItems = level.hasChunkAt(targetFluidPos())
            ? level.getCapability(Capabilities.ItemHandler.BLOCK, targetFluidPos(), facing) : null;
        // Use a shadow inventory so multiple item stacks cannot claim the same free slot.
        if (simulate && targetItems != null) {
            final var original = targetItems;
            var shadow = new net.neoforged.neoforge.items.ItemStackHandler(targetItems.getSlots()) {
                @Override public int getSlotLimit(int slot) { return original.getSlotLimit(slot); }
                @Override public boolean isItemValid(int slot, ItemStack stack) { return original.isItemValid(slot, stack); }
            };
            for (int slot = 0; slot < shadow.getSlots(); slot++) shadow.setStackInSlot(slot, targetItems.getStackInSlot(slot).copy());
            for (int slot = 0; slot < items.getSlots(); slot++) {
                var item = items.getStackInSlot(slot);
                if (!net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(targetItems, item.copy(), true).isEmpty()) return box.copy();
                // A generic shadow cannot reproduce custom insertion rules (virtual
                // drawers, unlimited slots, shared stores). Trust their own preflight;
                // execution already retains every unaccepted item.
                if (targetItems instanceof net.neoforged.neoforge.items.wrapper.InvWrapper
                    && !net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(shadow, item.copy(), false).isEmpty()) return box.copy();
            }
        }
        boolean hasItems = false;
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack item = items.getStackInSlot(slot).copy();
            ItemStack rest = targetItems == null ? item : simulate ? ItemStack.EMPTY
                : net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(targetItems, item, false);
            remainingItems.setStackInSlot(slot, rest);
            hasItems |= !rest.isEmpty();
        }
        var target = targetFluidHandler();
        if (simulate && targetItems == null && !dev.ccd.logistics.FluidBatchPlanning.fits(
            level, targetFluidPos(), facing, CompositePackageItem.fluids(box))) return box.copy();
        var fluids = new java.util.ArrayList<FluidStack>();
        for (FluidStack fluid : CompositePackageItem.fluids(box)) {
            // An item destination selects parcel separation; a fluid-only destination
            // retains direct unloading. Output uses Create's existing saved queue.
            if (targetItems != null) {
                if (!simulate) {
                    int remaining = fluid.getAmount();
                    while (remaining > 0) {
                        int amount = Math.min(FluidPackageItem.CAPACITY, remaining);
                        ItemStack parcel = FluidPackageItem.containingFluid(fluid.copyWithAmount(amount));
                        PackageItem.addAddress(parcel, PackageItem.getAddress(box));
                        queuedExitingPackages.add(new BigItemStack(parcel, 1));
                        computerBehaviour.prepareComputerEvent(new PackageEvent(parcel, "package_created"));
                        remaining -= amount;
                    }
                }
                continue;
            }
            FluidStack rest = target == null ? fluid : simulate
                ? (FluidTransfers.canFillAll(target, fluid) ? FluidStack.EMPTY : fluid)
                : FluidTransfers.fillAndKeepRemainder(target, fluid);
            if (!rest.isEmpty()) fluids.add(rest);
        }
        if (!hasItems && fluids.isEmpty()) return ItemStack.EMPTY;
        ItemStack remainder = CompositePackageItem.containing(remainingItems, fluids);
        PackageItem.addAddress(remainder, PackageItem.getAddress(box));
        return remainder;
    }

    @Override protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        filter = FluidStack.parseOptional(registries, tag.getCompound("FluidFilter"));
        recovery = FluidStack.parseOptional(registries, tag.getCompound("RecoveryFluid"));
        compositeRecovery = ItemStack.parseOptional(registries, tag.getCompound("CompositeRecovery"));
        status = tag.contains("FluidStatus") ? tag.getString("FluidStatus") : "ready";
        // Old saves may have been waiting for the removed exact-amount setting.
        if (status.equals("not_enough")) status = "ready";
        if (!clientPacket && tag.contains("FluidSummary"))
            fluidSummary = CatnipCodecUtils.decode(InventorySummary.CODEC, registries, tag.get("FluidSummary")).orElse(null);
    }
    @Override protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("FluidFilter", filter.saveOptional(registries));
        tag.put("RecoveryFluid", recovery.saveOptional(registries));
        tag.put("CompositeRecovery", compositeRecovery.saveOptional(registries));
        tag.putString("FluidStatus", status);
        if (!clientPacket && fluidSummary != null)
            tag.put("FluidSummary", CatnipCodecUtils.encode(InventorySummary.CODEC, registries, fluidSummary).orElseThrow());
    }

    @Override public void destroy() {
        if (level != null && !level.isClientSide() && !compositeRecovery.isEmpty())
            net.minecraft.world.level.block.Block.popResource(level, worldPosition, compositeRecovery.copy());
        compositeRecovery = ItemStack.EMPTY;
        if (level != null && !level.isClientSide() && !recovery.isEmpty()) {
            FluidStack remaining = recovery;
            recovery = FluidStack.EMPTY;
            while (!remaining.isEmpty()) {
                int amount = Math.min(FluidPackageItem.CAPACITY, remaining.getAmount());
                net.minecraft.world.level.block.Block.popResource(level, worldPosition,
                    FluidPackageItem.containingFluid(remaining.copyWithAmount(amount)));
                remaining = remaining.copyWithAmount(remaining.getAmount() - amount);
            }
        }
        recovery = FluidStack.EMPTY;
        super.destroy();
    }
    @Override public void invalidate(){super.invalidate();fluidEndpoint.clear();}
    @Override public void clearContent() {
        compositeRecovery = ItemStack.EMPTY;
        recovery = FluidStack.EMPTY;
        super.clearContent();
    }

    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CCDTooltip.heading(tooltip, "ccd.gui.goggles.packager");
        CCDTooltip.line(tooltip, status.equals("no_container") || status.equals("recovery") ? ChatFormatting.YELLOW : ChatFormatting.GRAY,
            "ccd.status." + status);
        if (!filter.isEmpty()) CCDTooltip.line(tooltip, "ccd.packager.filter_set", filter.getHoverName());
        if (!recovery.isEmpty()) CCDTooltip.line(tooltip, "ccd.packager.recovery_amount", recovery.getHoverName(), CCDTooltip.amount(recovery.getAmount()));
        return true;
    }
}
