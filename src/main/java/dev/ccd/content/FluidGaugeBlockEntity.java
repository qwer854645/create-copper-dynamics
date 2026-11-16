package dev.ccd.content;

import java.util.List;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class FluidGaugeBlockEntity extends FactoryPanelBlockEntity {
    public FluidGaugeBlockEntity(BlockPos pos, BlockState state) { super(CopperDynamics.FLUID_GAUGE_BE.get(), pos, state); }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.removeIf(behaviour -> behaviour instanceof FactoryPanelBehaviour);
        for (PanelSlot slot : PanelSlot.values()) {
            var fluid = new FluidGaugeBehaviour(this, slot);
            panels.put(slot, fluid);
            behaviours.add(fluid);
        }
    }
    @Override public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide()) return;
        BlockPos rear = worldPosition.relative(FactoryPanelBlock.connectedDirection(getBlockState()).getOpposite());
        boolean next = level.hasChunkAt(rear) && level.getBlockEntity(rear) instanceof FluidPackagerBlockEntity;
        if (restocker != next) { restocker = next; redraw = true; notifyUpdate(); }
    }
    @Override public PackagerBlockEntity getRestockedPackager() {
        if (!restocker || level == null) return null;
        BlockPos rear = worldPosition.relative(FactoryPanelBlock.connectedDirection(getBlockState()).getOpposite());
        return level.hasChunkAt(rear) && level.getBlockEntity(rear) instanceof FluidPackagerBlockEntity packager ? packager : null;
    }
    @Override public void destroy() {
        // Native FactoryPanelBlockEntity.destroy hardcodes ordinary factory-gauge drops.
        forEachBehaviour(BlockEntityBehaviour::destroy);
        if (level != null && !level.isClientSide() && activePanels() > 1)
            Block.popResource(level, worldPosition, new net.minecraft.world.item.ItemStack(CopperDynamics.FLUID_GAUGE_ITEM.get(), activePanels() - 1));
    }
}
