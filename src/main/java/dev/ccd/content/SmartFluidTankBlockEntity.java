package dev.ccd.content;

import java.util.ArrayList;
import java.util.List;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ccd.CCDConfig;
import dev.ccd.CopperDynamics;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/** Native structure search only; each member retains ownership of its actual fluids. */
public final class SmartFluidTankBlockEntity extends SmartBlockEntity
    implements IHaveGoggleInformation, IMultiBlockEntityContainer {
    private int syncCooldown;
    private boolean syncQueued, comparatorsQueued, rebuild = true;
    private BlockPos controller, lastKnownPos;
    private int width = 1, height = 1;
    private long structureValidatedAt = Long.MIN_VALUE;
    boolean storageBusy;
    @Nullable SmartTankClusterInventory.Snapshot cachedStorage;
    final SmartTankInventory localInventory = new SmartTankInventory(FluidTankBlockEntity::getCapacityMultiplier,
        () -> CCDConfig.SMART_TANK_MAX_FLUIDS.get(), this::contentsChanged);
    public final SmartTankClusterInventory inventory = new SmartTankClusterInventory(this);
    public SmartFluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(CopperDynamics.SMART_FLUID_TANK_BE.get(), pos, state);
        controller = lastKnownPos = pos;
    }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}
    @Override public BlockPos getController() { return controller; }
    @Override public boolean isController() { return worldPosition.equals(controller); }
    @Override public BlockPos getLastKnownPos() { return lastKnownPos; }
    @Override @SuppressWarnings("unchecked")
    @Nullable public SmartFluidTankBlockEntity getControllerBE() {
        if (level == null || isRemoved() || !level.hasChunkAt(worldPosition) || !level.hasChunkAt(controller)
            || level.getBlockEntity(worldPosition) != this) return null;
        if (level.getBlockEntity(controller) instanceof SmartFluidTankBlockEntity root && root.isController()
            && worldPosition.getX() >= controller.getX() && worldPosition.getX() < controller.getX() + root.width
            && worldPosition.getZ() >= controller.getZ() && worldPosition.getZ() < controller.getZ() + root.width
            && worldPosition.getY() >= controller.getY() && worldPosition.getY() < controller.getY() + root.height) return root;
        return null;
    }
    private boolean chunksLoaded() {
        if (level == null) return false;
        for (int x = 0; x < width; x++) for (int z = 0; z < width; z++)
            if (!level.hasChunkAt(controller.offset(x, 0, z))) return false;
        return true;
    }
    @Nullable List<SmartFluidTankBlockEntity> loadedParts() {
        if (!isController() || !chunksLoaded()) return null;
        if (cachedStorage != null && structureValidatedAt == level.getGameTime()
            && cachedStorage.parts().stream().allMatch(part -> !part.isRemoved()
                && level.getBlockEntity(part.worldPosition) == part && controller.equals(part.controller)
                && part.width == width && part.height == height)) return cachedStorage.parts();
        List<SmartFluidTankBlockEntity> result = new ArrayList<>();
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) for (int z = 0; z < width; z++) {
            BlockPos pos = controller.offset(x, y, z);
            if (!(level.getBlockState(pos).getBlock() instanceof SmartFluidTankBlock)
                || !(level.getBlockEntity(pos) instanceof SmartFluidTankBlockEntity part) || part.isRemoved()
                || !controller.equals(part.controller) || part.width != width || part.height != height) return null;
            result.add(part);
        }
        structureValidatedAt = level.getGameTime();
        return result;
    }
    private void contentsChanged() {
        setChanged();
        invalidateStorage();
        syncQueued = true;
        var root = getControllerBE();
        if (root != null) { root.cachedStorage = null; root.comparatorsQueued = true; }
    }
    private void invalidateStorage() {
        cachedStorage = null;
        if (level != null && level.hasChunkAt(controller)
            && level.getBlockEntity(controller) instanceof SmartFluidTankBlockEntity root) root.cachedStorage = null;
    }
    public void requestConnectivity() {
        var root = getControllerBE();
        if (root != null) { root.rebuild = true; root.cachedStorage = null; }
        cachedStorage = null;
        rebuild = true;
    }
    private boolean searchAreaLoaded() {
        // Fence the native search's neighbouring-controller lookups before invoking it.
        for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++)
            if (!level.hasChunkAt(worldPosition.offset(x, 0, z))) return false;
        // A moved/reloading member may still refer to a controller outside the search area.
        for (int x : new int[] {-8, 8}) for (int z : new int[] {-8, 8})
            for (var entity : level.getChunkAt(worldPosition.offset(x, 0, z)).getBlockEntities().values())
                if (entity instanceof SmartFluidTankBlockEntity tank
                    && Math.abs(tank.worldPosition.getX() - worldPosition.getX()) <= 4
                    && Math.abs(tank.worldPosition.getZ() - worldPosition.getZ()) <= 4
                    && !level.hasChunkAt(tank.controller)) return false;
        return true;
    }
    private void repairMissingStructure() {
        if (!chunksLoaded()) return;
        var root = getControllerBE();
        if (root != null && root.loadedParts() != null) return;
        BlockPos origin = controller;
        int oldWidth = width, oldHeight = height;
        for (int y = 0; y < oldHeight; y++) for (int x = 0; x < oldWidth; x++) for (int z = 0; z < oldWidth; z++)
            if (level.getBlockEntity(origin.offset(x, y, z)) instanceof SmartFluidTankBlockEntity part
                && origin.equals(part.controller)) part.removeController(true);
    }
    @Override public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;
        if (!worldPosition.equals(lastKnownPos)) { removeController(true); lastKnownPos = worldPosition; }
        if (rebuild || level.getGameTime() % 20 == Math.floorMod(worldPosition.hashCode(), 20)) repairMissingStructure();
        if (rebuild && isController() && chunksLoaded() && searchAreaLoaded()) {
            rebuild = false;
            ConnectivityHandler.formMulti(this);
        }
        if (syncCooldown > 0) syncCooldown--;
        if (syncQueued && syncCooldown == 0) { sendData(); syncQueued = false; syncCooldown = 8; }
        if (comparatorsQueued) {
            comparatorsQueued = false;
            var parts = loadedParts();
            if (parts != null) for (var part : parts)
                level.updateNeighbourForOutputSignal(part.worldPosition, part.getBlockState().getBlock());
        }
    }
    public void splitBeforeRemoval() {
        if (level == null || level.isClientSide()) return;
        var root = getControllerBE();
        if (root != null) root.cachedStorage = null;
        if (root != null && root.chunksLoaded()) ConnectivityHandler.splitMulti(root);
    }
    @Override public void setController(BlockPos pos) {
        invalidateStorage();
        controller = pos.immutable(); cachedStorage = null; invalidateCapabilities(); setChanged();
    }
    @Override public void removeController(boolean keepContents) {
        // Structural splitting never clears or migrates local multi-fluid ownership.
        invalidateStorage();
        controller = worldPosition; width = height = 1; rebuild = true; notifyMultiUpdated();
    }
    @Override public void preventConnectivityUpdate() { rebuild = false; }
    @Override public Direction.Axis getMainConnectionAxis() { return Direction.Axis.Y; }
    @Override public int getMaxWidth() { return FluidTankBlockEntity.getMaxSize(); }
    @Override public int getMaxLength(Direction.Axis axis, int width) { return FluidTankBlockEntity.getMaxHeight(); }
    @Override public int getWidth() { return width; }
    @Override public int getHeight() { return height; }
    @Override public void setWidth(int value) { width = value; }
    @Override public void setHeight(int value) { height = value; }
    @Override public void notifyMultiUpdated() {
        cachedStorage = null; invalidateCapabilities(); comparatorsQueued = true;
        invalidateRenderBoundingBox();
        if (level == null || level.isClientSide()) return;
        int x = worldPosition.getX() - controller.getX(), z = worldPosition.getZ() - controller.getZ();
        FluidTankBlock.Shape shape = FluidTankBlock.Shape.PLAIN;
        if (width == 1 || width == 3 && Math.abs(Math.abs(x) - Math.abs(z)) == 1) shape = FluidTankBlock.Shape.WINDOW;
        if (width == 2) shape = x == 0 ? z == 0 ? FluidTankBlock.Shape.WINDOW_NW : FluidTankBlock.Shape.WINDOW_SW
            : z == 0 ? FluidTankBlock.Shape.WINDOW_NE : FluidTankBlock.Shape.WINDOW_SE;
        if (level.getBlockState(worldPosition).getBlock() instanceof SmartFluidTankBlock)
            level.setBlock(worldPosition, getBlockState()
                .setValue(FluidTankBlock.BOTTOM, worldPosition.getY() == controller.getY())
                .setValue(FluidTankBlock.TOP, worldPosition.getY() == controller.getY() + height - 1)
                .setValue(FluidTankBlock.SHAPE, shape), Block.UPDATE_CLIENTS);
        setChanged(); sendData();
    }
    @Override protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Fluids", localInventory.save(registries));
        tag.put("Controller", NbtUtils.writeBlockPos(controller));
        tag.put("LastKnownPos", NbtUtils.writeBlockPos(lastKnownPos));
        tag.putInt("Width", width); tag.putInt("Height", height);
    }
    @Override protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        localInventory.load(tag.getList("Fluids", Tag.TAG_COMPOUND), registries);
        controller = tag.contains("Controller") ? NBTHelper.readBlockPos(tag, "Controller") : worldPosition;
        lastKnownPos = tag.contains("LastKnownPos") ? NBTHelper.readBlockPos(tag, "LastKnownPos") : worldPosition;
        width = Math.clamp(tag.getInt("Width"), 1, 3);
        height = Math.clamp(tag.getInt("Height"), 1, 4096);
        invalidateRenderBoundingBox();
        cachedStorage = null;
        var root = getControllerBE();
        if (root != null) root.cachedStorage = null;
        rebuild = !clientPacket;
    }
    @Override protected AABB createRenderBoundingBox() {
        return new AABB(worldPosition).expandTowards(width - 1, height - 1, width - 1);
    }
    @Override public void destroy() {
        super.destroy();
        for (var fluid : localInventory.takeAll()) if (level != null && !level.isClientSide()) {
            int packetSize = Math.max(1, Math.min(FluidPackageItem.CAPACITY, localInventory.capacity()));
            while (!fluid.isEmpty()) {
                int amount = Math.min(packetSize, fluid.getAmount());
                Block.popResource(level, worldPosition, FluidPackageItem.containingFluid(fluid.copyWithAmount(amount)));
                fluid.shrink(amount);
            }
        }
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        tooltip.add(Component.translatable("block.ccd.smart_fluid_tank").withStyle(ChatFormatting.GOLD));
        var view = inventory.snapshot();
        if (view == null) {
            tooltip.add(Component.translatable("ccd.smart_tank.unavailable").withStyle(ChatFormatting.GRAY));
            return true;
        }
        tooltip.add(Component.translatable("ccd.smart_tank.structure", width, height, view.parts().size()));
        tooltip.add(Component.translatable("ccd.smart_tank.total", view.amount(), view.capacity()));
        tooltip.add(Component.translatable("ccd.smart_tank.types", view.kinds(), CCDConfig.SMART_TANK_MAX_FLUIDS.get()));
        for (var fluid : view.fluids()) tooltip.add(Component.translatable("ccd.smart_tank.fluid", fluid.getHoverName(), fluid.getAmount())
            .withStyle(ChatFormatting.GRAY));
        return true;
    }
}
