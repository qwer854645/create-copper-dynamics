package dev.ccd.content;

import java.util.List;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ccd.CCDConfig;
import dev.ccd.CopperDynamics;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class SmartFluidTankBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private int syncCooldown;
    private boolean syncQueued;
    public final SmartTankInventory inventory = new SmartTankInventory(FluidTankBlockEntity::getCapacityMultiplier,
        () -> CCDConfig.SMART_TANK_MAX_FLUIDS.get(), this::contentsChanged);
    public SmartFluidTankBlockEntity(BlockPos pos, BlockState state) { super(CopperDynamics.SMART_FLUID_TANK_BE.get(), pos, state); }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}
    private void contentsChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            syncQueued = true;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }
    @Override public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;
        if (syncCooldown > 0) syncCooldown--;
        if (syncQueued && syncCooldown == 0) {
            sendData();
            syncQueued = false;
            syncCooldown = 8;
        }
    }
    @Override protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Fluids", inventory.save(registries));
    }
    @Override protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        inventory.load(tag.getList("Fluids", Tag.TAG_COMPOUND), registries);
    }
    @Override public void destroy() {
        super.destroy();
        // Use the existing sealed carrier to recover each actual fluid once; no extra item inventory.
        for (var fluid : inventory.takeAll()) if (level != null && !level.isClientSide()) {
            int packetSize = Math.max(1, Math.min(FluidPackageItem.CAPACITY, inventory.capacity()));
            while (!fluid.isEmpty()) {
                int amount = Math.min(packetSize, fluid.getAmount());
                Block.popResource(level, worldPosition, FluidPackageItem.containingFluid(fluid.copyWithAmount(amount)));
                fluid.shrink(amount);
            }
        }
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        tooltip.add(Component.translatable("block.ccd.smart_fluid_tank").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("ccd.smart_tank.total", inventory.amount(), inventory.capacity()));
        tooltip.add(Component.translatable("ccd.smart_tank.types", inventory.kinds(), CCDConfig.SMART_TANK_MAX_FLUIDS.get()));
        for (int slot = 0; slot < inventory.getTanks(); slot++) {
            var fluid = inventory.getFluidInTank(slot);
            if (!fluid.isEmpty()) tooltip.add(Component.translatable("ccd.smart_tank.fluid", fluid.getHoverName(), fluid.getAmount())
                .withStyle(ChatFormatting.GRAY));
        }
        return true;
    }
}
