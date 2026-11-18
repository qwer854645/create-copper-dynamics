package dev.ccd.content;

import java.util.ArrayList;
import java.util.List;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import dev.ccd.CCDConfig;
import dev.ccd.CCDTooltip;
import dev.ccd.CopperDynamics;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/** Native tank entity with controller-owned multi-fluid storage and multi-fluid migration hooks. */
public class SmartFluidTankBlockEntity extends FluidTankBlockEntity {
    private boolean syncQueued, fluidUpdatePending;
    private boolean splitPrepared;
    private static final ThreadLocal<Boolean> RESTRUCTURING = ThreadLocal.withInitial(() -> false);
    boolean storageBusy;
    @Nullable SmartTankClusterInventory.Snapshot cachedStorage;
    final SmartTankInventory storedInventory = new SmartTankInventory(this::ownedCapacity,
        () -> CCDConfig.SMART_TANK_MAX_FLUIDS.get(), this::contentsChanged);
    public final SmartTankClusterInventory inventory = new SmartTankClusterInventory(this);
    public SmartFluidTankBlockEntity(BlockPos pos, BlockState state) {
        this(CopperDynamics.SMART_FLUID_TANK_BE.get(), pos, state);
    }
    protected SmartFluidTankBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type,pos,state); }
    public BlockPos structureOffset(int x,int y,int z) { return new BlockPos(x,y,z); }
    @Override protected SmartFluidTank createInventory() {
        // The parent's single-fluid field must not become a second owner or participate in connectivity migration.
        return new SmartFluidTank(0, fluid -> {});
    }
    @Override public boolean hasTank() { return false; }
    public boolean hasWindow() { return window; }
    @Override public void updateBoilerState() { boiler.clear(); }
    @Override public float getFillState() {
        return storedInventory == null || ownedCapacity() <= 0 ? 0 : Math.min(1f, (float) storedInventory.amount() / ownedCapacity());
    }
    @Override protected void onFluidStackChanged(FluidStack fluid) { fluidUpdatePending = true; }
    static boolean isRestructuring() { return RESTRUCTURING.get(); }
    private static void changeStructure(Runnable action) {
        boolean nested = RESTRUCTURING.get();
        RESTRUCTURING.set(true);
        try { action.run(); }
        finally { if (nested) RESTRUCTURING.set(true); else RESTRUCTURING.remove(); }
    }
    private int ownedCapacity() {
        long blocks = isController() ? (long) width * width * height : 1;
        return (int) Math.min(Integer.MAX_VALUE, blocks * FluidTankBlockEntity.getCapacityMultiplier());
    }
    @Override public SmartFluidTankBlockEntity getControllerBE() {
        var root = super.getControllerBE();
        return root instanceof SmartFluidTankBlockEntity smart ? smart : null;
    }
    private void contentsChanged() {
        setChanged();
        invalidateStorage();
        syncQueued = true;
        var root = getControllerBE();
        if (root != null) { root.cachedStorage = null; root.fluidUpdatePending = true; }
    }
    private void invalidateStorage() {
        cachedStorage = null;
        if (level != null && level.hasChunkAt(getController())
            && level.getBlockEntity(getController()) instanceof SmartFluidTankBlockEntity root) root.cachedStorage = null;
    }
    private List<SmartFluidTankBlockEntity> linkedParts() {
        List<SmartFluidTankBlockEntity> parts = new ArrayList<>();
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) for (int z = 0; z < width; z++) {
            BlockPos pos = getController().offset(structureOffset(x, y, z));
            if (level.getBlockEntity(pos) instanceof SmartFluidTankBlockEntity part
                && getController().equals(part.getController())) parts.add(part);
        }
        return parts;
    }
    private void distribute(List<SmartFluidTankBlockEntity> sources, List<SmartFluidTankBlockEntity> destinations) {
        List<FluidStack> payload = new ArrayList<>(storedInventory.takeAll());
        for (var part : sources) if (part != this) payload.addAll(part.storedInventory.takeAll());
        int index = 0, free = FluidTankBlockEntity.getCapacityMultiplier();
        for (var fluid : payload) {
            int left = fluid.getAmount();
            while (left > 0 && index < destinations.size()) {
                int moved = Math.min(left, free);
                destinations.get(index).storedInventory.absorbOwned(List.of(fluid.copyWithAmount(moved)));
                left -= moved;
                free -= moved;
                if (free == 0) { index++; free = FluidTankBlockEntity.getCapacityMultiplier(); }
            }
            // Native shrinking discards anything that no longer fits; do not create recovery items.
        }
        splitPrepared = true;
    }
    @Override public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;
        if (fluidUpdatePending && isController()) {
            fluidUpdatePending = false;
            FluidStack brightest = FluidStack.EMPTY;
            for (int i = 0; i < storedInventory.getTanks(); i++) {
                FluidStack fluid = storedInventory.getFluidInTank(i);
                if (!fluid.isEmpty() && (brightest.isEmpty() || fluid.getFluidType().getLightLevel(fluid)
                    > brightest.getFluidType().getLightLevel(brightest))) brightest = fluid;
            }
            // Reuse native comparator, lighting, fluid-level and throttled synchronization updates.
            super.onFluidStackChanged(brightest);
        }
        if (syncQueued) { syncQueued = false; sendData(); }
    }
    @Override protected void updateConnectivity() {
        changeStructure(() -> super.updateConnectivity());
    }
    public void splitAfterRemoval(@Nullable SmartFluidTankBlockEntity root) {
        if (level == null || level.isClientSide()) return;
        if (root != null) root.cachedStorage = null;
        if (root != null) changeStructure(() -> {
            // Native split also visits the full original bounds, rather than treating absent chunks as missing tanks.
            var sources = root.linkedParts();
            var survivors = sources.stream().filter(part -> part != this
                && level.getBlockState(part.worldPosition).getBlock() instanceof SmartFluidTankBlock).toList();
            root.distribute(sources, survivors);
            ConnectivityHandler.splitMulti(root);
        });
    }
    @Override public void setController(BlockPos pos) {
        invalidateStorage();
        if (!pos.equals(worldPosition) && level != null && level.hasChunkAt(pos)
            && level.getBlockEntity(pos) instanceof SmartFluidTankBlockEntity target)
            target.storedInventory.absorbOwned(storedInventory.takeAll());
        super.setController(pos);
        cachedStorage = null;
    }
    @Override public void removeController(boolean keepContents) {
        if (isController() && !splitPrepared && (width > 1 || height > 1)) {
            var parts = linkedParts();
            distribute(parts, parts);
        }
        invalidateStorage();
        super.removeController(keepContents);
        cachedStorage = null;
        splitPrepared = false;
    }
    @Override public void notifyMultiUpdated() {
        cachedStorage = null; invalidateCapabilities(); fluidUpdatePending = true;
        invalidateRenderBoundingBox();
        if (level == null || level.isClientSide()) return;
        super.notifyMultiUpdated();
    }
    @Override public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.remove("TankContent");
        if (isController()) tag.put("Fluids", storedInventory.save(registries));
    }
    @Override protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        storedInventory.load(isController() ? tag.getList("Fluids", Tag.TAG_COMPOUND) : new ListTag(), registries);
        boiler.clear();
        cachedStorage = null;
        var root = getControllerBE();
        if (root != null) root.cachedStorage = null;
        fluidUpdatePending = true;
    }
    @Override public void destroy() {
        storedInventory.takeAll();
        super.destroy();
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        var view = inventory.snapshot();
        if (view == null) {
            CCDTooltip.heading(tooltip, "create.gui.goggles.fluid_container");
            CCDTooltip.line(tooltip, ChatFormatting.YELLOW, "ccd.smart_tank.unavailable");
            return true;
        }
        CCDTooltip.fluidContents(tooltip, view.fluids(), view.capacity());
        var root = getControllerBE();
        CCDTooltip.line(tooltip, "ccd.smart_tank.total", CCDTooltip.amount(view.amount()), CCDTooltip.amount(view.capacity()));
        CCDTooltip.line(tooltip, "ccd.smart_tank.types", CCDTooltip.number(view.kinds()), CCDTooltip.number(CCDConfig.SMART_TANK_MAX_FLUIDS.get()));
        if (sneaking) CCDTooltip.line(tooltip, "ccd.smart_tank.structure", CCDTooltip.number(root.getWidth()),
            CCDTooltip.number(root.getHeight()), CCDTooltip.number(view.blocks()));
        return true;
    }
}
