package dev.ccd.content;

import java.util.List;
import com.simibubi.create.api.behaviour.spouting.CauldronSpoutingBehavior;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.FluidFX;
import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ccd.CopperDynamics;
import dev.ccd.CCDTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;

/** A basic container-fed faucet. No internal tank, item-processing behaviour or exposed fluid inventory. */
public class FaucetBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final int LIMIT = 1_000;
    private int cooldown;
    protected boolean transferring;
    private FluidStack flowing = FluidStack.EMPTY;
    private int flowTicks, flowDistance=1;
    public FluidStack flowingFluid() { return enabled() && flowTicks>0 ? flowing : FluidStack.EMPTY; }
    public int flowDistance() { return flowDistance; }
    public FaucetBlockEntity(BlockPos pos, BlockState state) { this(CopperDynamics.FAUCET_BE.get(), pos, state); }
    protected FaucetBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    protected boolean enabled() {
        return getBlockState().getValue(FaucetBlock.OPEN) && !getBlockState().getValue(FaucetBlock.POWERED);
    }
    protected boolean acceptsFluid(FluidStack fluid) { return true; }
    protected boolean allowContainerTransfer() { return true; }
    @Nullable protected IFluidHandler inputHandler() {
        if (level == null) return null;
        Direction front = getBlockState().getValue(FaucetBlock.FACING);
        BlockPos input = worldPosition.relative(front.getOpposite());
        return level.hasChunkAt(input) ? level.getCapability(Capabilities.FluidHandler.BLOCK, input, front) : null;
    }
    protected void returnUndelivered(IFluidHandler source, FluidStack fluid) {
        FluidStack rest = FluidTransfers.fillAndKeepRemainder(source, fluid);
        if (!rest.isEmpty()) Containers.dropItemStack(level, worldPosition.getX() + .5,
            worldPosition.getY() + .5, worldPosition.getZ() + .5, FluidPackageItem.containingFluid(rest));
    }
    protected void splash(FluidStack fluid, int distance) {
        flowing=fluid.copyWithAmount(1);
        flowDistance=distance;
        flowTicks=SpoutBlockEntity.FILLING_TIME;
        notifyUpdate();
        if (level instanceof ServerLevel server) server.sendParticles(FluidFX.getFluidParticle(fluid),
            worldPosition.getX() + .5, worldPosition.getY() + .1 - (distance - 1), worldPosition.getZ() + .5,
            6, .04, .1, .04, .02);
    }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}
    @Override public void initialize() {
        super.initialize();
        if (level != null) FaucetBlock.updateSignal(level, worldPosition);
    }
    @Override public void tick() {
        super.tick();
        if (flowTicks>0) flowTicks--;
        if (!(level instanceof ServerLevel) || transferring || isRemoved()) return;
        if (!enabled()) { cooldown = 0; return; }
        if (!allowContainerTransfer()) return;
        if (cooldown > 0) { cooldown--; return; }
        cooldown = SpoutBlockEntity.FILLING_TIME - 1;
        BlockPos output = worldPosition.below();
        if (!level.hasChunkAt(output)) return;
        var source = inputHandler();
        if (source == null) return;
        var target = level.getCapability(Capabilities.FluidHandler.BLOCK, output, Direction.UP);
        if (source == target) return;
        transferring = true;
        try {
            for (int tank = 0; tank < source.getTanks(); tank++) {
                FluidStack sample = source.getFluidInTank(tank).copy();
                if (sample.isEmpty() || !acceptsFluid(sample)) continue;
                FluidStack planned = FluidTransfers.planDrain(source, sample, LIMIT, false);
                if (planned.isEmpty()) continue;
                int fits = Math.clamp(target != null ? target.fill(planned.copy(), FluidAction.SIMULATE)
                    : fillCauldron(output, planned, true), 0, planned.getAmount());
                if (fits == 0) continue;
                planned.setAmount(fits);
                FluidStack confirmed = source.drain(planned.copy(), FluidAction.SIMULATE);
                if (!FluidStack.isSameFluidSameComponents(confirmed, planned) || confirmed.getAmount() < fits) continue;
                FluidStack actual = source.drain(planned.copy(), FluidAction.EXECUTE);
                if (actual.isEmpty()) return;
                int accepted = 0;
                if (FluidStack.isSameFluidSameComponents(actual, planned) && actual.getAmount() <= fits)
                    accepted = Math.clamp(target != null ? target.fill(actual.copy(), FluidAction.EXECUTE)
                        : fillCauldron(output, actual, false), 0, actual.getAmount());
                FluidStack remainder = accepted == actual.getAmount() ? FluidStack.EMPTY
                    : actual.copyWithAmount(actual.getAmount() - accepted);
                if (!remainder.isEmpty()) returnUndelivered(source, remainder);
                if (accepted > 0) splash(actual, 1);
                return;
            }
        } finally {
            transferring = false;
        }
    }
    /** Reuse Create's public cauldron fluid/amount registry, not a fake SpoutBlockEntity or null API context. */
    private int fillCauldron(BlockPos pos, FluidStack fluid, boolean simulate) {
        var info = CauldronSpoutingBehavior.CAULDRON_INFO.get(fluid.getFluid());
        if (info == null || info.amount() <= 0 || info.amount() > fluid.getAmount()) return 0;
        BlockState current = level.getBlockState(pos);
        BlockState next;
        if (current.is(Blocks.CAULDRON)) next = info.cauldron();
        else if (current.is(info.cauldron().getBlock()) && current.getBlock() instanceof LayeredCauldronBlock
            && current.hasProperty(LayeredCauldronBlock.LEVEL)) {
            int amount = current.getValue(LayeredCauldronBlock.LEVEL);
            if (amount >= 3) return 0;
            next = current.setValue(LayeredCauldronBlock.LEVEL, amount + 1);
        } else return 0;
        return simulate || level.setBlockAndUpdate(pos, next) ? info.amount() : 0;
    }
    @Override protected void write(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag,registries,clientPacket);
        if(clientPacket) {
            tag.put("FlowingFluid",flowing.saveOptional(registries));
            tag.putInt("FlowTicks",flowTicks); tag.putInt("FlowDistance",flowDistance);
        }
    }
    @Override protected void read(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag,registries,clientPacket);
        if(clientPacket) {
            flowing=FluidStack.parseOptional(registries,tag.getCompound("FlowingFluid"));
            flowTicks=tag.getInt("FlowTicks"); flowDistance=Math.clamp(tag.getInt("FlowDistance"),1,2);
        }
    }
    @Override protected net.minecraft.world.phys.AABB createRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(worldPosition).expandTowards(0,-2,0);
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        String mode = getBlockState().getValue(FaucetBlock.POWERED) ? "paused"
            : getBlockState().getValue(FaucetBlock.OPEN) ? "open" : "closed";
        CCDTooltip.heading(tooltip, "ccd.gui.goggles.faucet");
        CCDTooltip.line(tooltip, mode.equals("paused") ? ChatFormatting.YELLOW
            : mode.equals("open") ? ChatFormatting.AQUA : ChatFormatting.GRAY, "ccd.faucet." + mode);
        return true;
    }
}
