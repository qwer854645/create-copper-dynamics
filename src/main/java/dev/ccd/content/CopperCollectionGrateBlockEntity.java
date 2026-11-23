package dev.ccd.content;

import java.util.List;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import dev.ccd.CopperDynamics;
import dev.ccd.CCDTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Floor collector: native incremental world drainage, native tank behaviour and gravity output. */
public class CopperCollectionGrateBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final int CAPACITY = 2000;
    private static final int INTERVAL = 5;
    private SmartFluidTankBehaviour tank;
    private FluidDrainingBehaviour drainer;
    private Fluid sourceFluid = Fluids.EMPTY;
    private BlockPos sourcePos;
    private BlockCapabilityCache<IFluidHandler, Direction> outlet;
    private boolean transferring;

    public CopperCollectionGrateBlockEntity(BlockPos pos, BlockState state) {
        this(CopperDynamics.COPPER_COLLECTION_GRATE_BE.get(), pos, state);
    }
    protected CopperCollectionGrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(tank = SmartFluidTankBehaviour.single(this, CAPACITY).whenFluidUpdates(() -> {
            if (level != null && !level.isClientSide()) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }));
        inventory().setValidator(this::acceptsFluid);
        behaviours.add(drainer = new FluidDrainingBehaviour(this) {
            @Override public void tick() {
        if (!dev.ccd.CCDFeatures.enabled(getBlockState())) return;
                // Preserve Create's incremental search budget, but run it only on scheduled work ticks with room available.
                if (workTick() && canCollectArea() && worldPosition.above().equals(sourcePos)
                    && sourceFluid.isSame(FluidHelper.convertToStill(level.getFluidState(worldPosition.above()).getType()))) super.tick();
            }
        });
        // Basins use this behaviour to discover side-output destinations. No item capability means items stay in the basin.
        behaviours.add(new DirectBeltInputBehaviour(this).onlyInsertWhen(side -> side.getAxis().isHorizontal()));
        registerAwardables(behaviours, AllAdvancements.HOSE_PULLEY, AllAdvancements.HOSE_PULLEY_LAVA);
    }
    public SmartFluidTank inventory() { return tank.getPrimaryHandler(); }
    public SmartFluidTankBehaviour tankBehaviour() { return tank; }
    protected boolean automaticEnabled() { return true; }
    protected boolean acceptsFluid(FluidStack fluid) { return true; }
    private boolean workTick() {
        return level instanceof ServerLevel && automaticEnabled()
            && Math.floorMod(level.getGameTime() + worldPosition.asLong(), INTERVAL) == 0;
    }
    private boolean canCollectArea() {
        if (inventory().getSpace() < 1000 || !level.hasChunkAt(worldPosition.above())) return false;
        var source = level.getFluidState(worldPosition.above());
        return !source.isEmpty() && acceptsFluid(new FluidStack(FluidHelper.convertToStill(source.getType()), 1000)) && (inventory().isEmpty()
            || inventory().getFluid().getFluid().isSame(FluidHelper.convertToStill(source.getType())));
    }
    public void collectEnvironmental(Fluid fluid, int amount) {
        if (!automaticEnabled() || amount <= 0 || amount > CAPACITY) return;
        var sample = new FluidStack(fluid, amount);
        if (inventory().fill(sample, FluidAction.SIMULATE) == amount) inventory().fill(sample, FluidAction.EXECUTE);
    }
    @Override public void tick() {
        super.tick();
        if (!workTick() || isRemoved() || transferring) return;
        pushDown();
        if (collectPowderSnow()) return;
        if (!canCollectArea()) return;
        BlockPos root = worldPosition.above();
        Fluid fluid = FluidHelper.convertToStill(level.getFluidState(root).getType());
        if (!root.equals(sourcePos) || !fluid.isSame(sourceFluid)) {
            drainer.reset();
            sourcePos = root;
            sourceFluid = fluid;
        }
        // pullNext(simulate) initializes native search context even before a fluid has been discovered.
        drainer.pullNext(root, true);
        FluidStack available = drainer.getDrainableFluid(root);
        if (!available.isEmpty() && inventory().fill(available, FluidAction.SIMULATE) == available.getAmount()
            && drainer.pullNext(root, false)) inventory().fill(available, FluidAction.EXECUTE);
    }
    private boolean collectPowderSnow() {
        BlockPos above = worldPosition.above();
        if (!level.hasChunkAt(above) || !level.getBlockState(above).is(net.minecraft.world.level.block.Blocks.POWDER_SNOW)) return false;
        // Solid snow has no FluidState. A temporary handler avoids caching a capability on a vanilla block.
        if (inventory().getSpace() >= 1000) FluidUtil.tryFluidTransfer(inventory(), new PowderSnowPickupHandler(level, above), 1000, true);
        return true;
    }
    private void pushDown() {
        if (!(level instanceof ServerLevel server) || inventory().isEmpty()) return;
        BlockPos below = worldPosition.below();
        if (outlet == null || !outlet.pos().equals(below) || outlet.level() != server)
            outlet = BlockCapabilityCache.create(Capabilities.FluidHandler.BLOCK, server, below, Direction.UP,
                () -> !isRemoved() && level == server && worldPosition.below().equals(below), () -> {});
        IFluidHandler target = outlet.getCapability();
        if (target == null || target == tank.getCapability() || target == inventory()) return;
        transferring = true;
        try { FluidUtil.tryFluidTransfer(target, tank.getCapability(), 1000, true); }
        finally { transferring = false; }
    }
    @Override public void invalidate() {
        super.invalidate();
        outlet = null;
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        containedFluidTooltip(tooltip, sneaking, tank.getCapability());
        if (drainer.isInfinite()) CCDTooltip.line(tooltip, ChatFormatting.AQUA, "ccd.collection_grate.infinite");
        return true;
    }
}
