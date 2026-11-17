package dev.ccd.content;

import java.util.List;
import java.util.function.Consumer;
import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity.CreativeSmartFluidTank;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import dev.ccd.CopperDynamics;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** Reuse native creative draining and input disposal; only the fluid identity and capacity are fixed. */
public class CopperWaterBasinBlockEntity extends FluidTankBlockEntity {
    public static final int CAPACITY = 1_000_000;

    public CopperWaterBasinBlockEntity(BlockPos pos, BlockState state) {
        this(CopperDynamics.COPPER_WATER_BASIN_BE.get(), pos, state);
    }
    protected CopperWaterBasinBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    // Keep the regular native entity: FluidHelper deliberately duplicates containers for creative-tank entities.
    // Only the inventory needs creative behavior; survival buckets and bottles must still be consumed normally.
    @Override protected SmartFluidTank createInventory() { return new FixedWaterTank(this::onFluidStackChanged); }
    // This container is always one block; skip native multiblock formation rather than creating zero-height tanks.
    @Override protected void updateConnectivity() { updateConnectivity = false; }
    @Override public int getMaxWidth() { return 1; }
    @Override public int getMaxLength(Direction.Axis axis, int width) { return 1; }
    @Override public void updateBoilerState() { boiler.clear(); }
    @Override public void toggleWindows() {} // An open basin always displays its water.

    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        tooltip.add(getBlockState().getBlock().getName().withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("ccd.water_basin.infinite").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("ccd.water_basin.capacity", CAPACITY).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("ccd.water_basin.disposal").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("ccd.water_basin.interaction").withStyle(ChatFormatting.GRAY));
        return true;
    }

    private static final class FixedWaterTank extends CreativeSmartFluidTank {
        private FixedWaterTank(Consumer<FluidStack> changed) {
            super(CAPACITY, changed);
            // The native entity calls createInventory from its constructor, before its other fields exist.
            fluid = water();
        }
        private static FluidStack water() { return new FluidStack(Fluids.WATER, CAPACITY); }

        @Override public FixedWaterTank setCapacity(int ignored) {
            super.setCapacity(CAPACITY);
            return this;
        }
        @Override public void setFluid(FluidStack ignored) { super.setFluid(water()); }
        // Native creative-tank bucket interaction sets the input fluid, or clears it after filling an item.
        @Override public void setContainedFluid(FluidStack ignored) { super.setContainedFluid(water()); }
        @Override public FixedWaterTank readFromNBT(HolderLookup.Provider registries, CompoundTag tag) {
            // Water is intrinsic to the block, rather than configurable saved cargo.
            fluid = water();
            return this;
        }
        // fill and both drain overloads deliberately use Create's unchanged creative-tank implementation.
    }
}
