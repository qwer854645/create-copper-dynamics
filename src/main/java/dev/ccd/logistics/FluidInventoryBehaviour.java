package dev.ccd.logistics;

import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.CapManipulationBehaviourBase.InterfaceProvider;
import dev.ccd.content.FluidPackagerBlockEntity;
import dev.ccd.content.FluidStockItem;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

/** Read-only identity view used ONLY by Create's logistics behaviour, never exposed
 * as a world item capability. Actual extraction uses the standard fluid interface. */
public final class FluidInventoryBehaviour extends InvManipulationBehaviour {
    private final FluidPackagerBlockEntity packager;
    private final IItemHandler view = new IItemHandler() {
        @Override public int getSlots() {
            var fluids = packager.targetFluidHandler();
            return fluids == null ? 0 : fluids.getTanks();
        }
        @Override public ItemStack getStackInSlot(int slot) {
            var fluids = packager.targetFluidHandler();
            return fluids == null || slot < 0 || slot >= fluids.getTanks() ? ItemStack.EMPTY : FluidStockItem.of(fluids.getFluidInTank(slot));
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
    };

    public FluidInventoryBehaviour(FluidPackagerBlockEntity packager) {
        super(packager, InterfaceProvider.oppositeOfBlockFacing());
        this.packager = packager;
    }
    @Override public boolean hasInventory() { return packager.targetFluidHandler() != null; }
    @Override public IItemHandler getInventory() { return hasInventory() ? view : null; }
    @Override public void findNewCapability() {} // Do not query an unrelated item inventory.
    @Override public IdentifiedInventory getIdentifiedInventory() {
        if (!hasInventory()) return null;
        var level = packager.getLevel();
        BlockPos target = packager.targetFluidPos();
        var sources = FluidAccess.resolve(level, target,
            packager.getBlockState().getValue(com.simibubi.create.content.logistics.packager.PackagerBlock.FACING));
        var identities = sources.stream().map(FluidAccess.Source::identity).collect(java.util.stream.Collectors.toSet());
        return new IdentifiedInventory(new AccessId(level.dimension(), identities, packager.getBlockPos()), view);
    }
    public record AccessId(ResourceKey<Level> dimension, java.util.Set<BlockPos> sources, BlockPos access) implements InventoryIdentifier {
        @Override public boolean contains(BlockFace face) { return sources.contains(face.getPos()); }
    }
    public record StorageId(ResourceKey<Level> dimension, BlockPos pos) implements InventoryIdentifier {
        @Override public boolean contains(BlockFace face) { return pos.equals(face.getPos()); }
    }
}
