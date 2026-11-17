package dev.ccd.content;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import dev.ccd.CCDConfig;
import dev.ccd.CopperDynamics;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** Six cached sided endpoints; only scheduled supply operations inspect the target inventories. */
public final class CopperWaterSupplyBlockEntity extends CopperWaterBasinBlockEntity {
    private static final int ACTIVE_INTERVAL = 5, IDLE_INTERVAL = 20;
    private record Target(BlockCapabilityCache<IFluidHandler, Direction> cache) {}
    private final List<Target> targets = new ArrayList<>();
    private BlockPos cacheOrigin;
    private long nextSupplyTick, lastSupplyTick;

    public CopperWaterSupplyBlockEntity(BlockPos pos, BlockState state) {
        super(CopperDynamics.COPPER_WATER_SUPPLY_BE.get(), pos, state);
    }
    private void ensureCaches(ServerLevel server) {
        if (worldPosition.equals(cacheOrigin) && targets.size() == 6 && targets.getFirst().cache.level() == server) return;
        targets.clear();
        cacheOrigin = worldPosition.immutable();
        BlockPos origin = cacheOrigin;
        for (Direction side : Direction.values()) targets.add(new Target(BlockCapabilityCache.create(
            Capabilities.FluidHandler.BLOCK, server, origin.relative(side), side.getOpposite(),
            () -> !isRemoved() && level == server && worldPosition.equals(origin),
            // Coalesce notifications without level access, while retaining the 5-tick minimum work interval.
            () -> nextSupplyTick = Math.min(nextSupplyTick, lastSupplyTick + ACTIVE_INTERVAL))));
    }
    @Override public void invalidate() {
        super.invalidate();
        targets.clear();
        cacheOrigin = null;
    }
    @Override public void tick() {
        super.tick();
        if (!(level instanceof ServerLevel server) || isRemoved() || getBlockState().getValue(CopperWaterSupplyBlock.POWERED)) return;
        int rate = CCDConfig.WATER_SUPPLY_RATE.get();
        if (rate == 0 || server.getGameTime() < nextSupplyTick) return;
        lastSupplyTick = server.getGameTime();
        ensureCaches(server);
        int budget = rate == -1 ? Integer.MAX_VALUE : (int) Math.min(Integer.MAX_VALUE, (long) rate * ACTIVE_INTERVAL);
        Set<BlockPos> controllers = new HashSet<>();
        Set<IFluidHandler> handlers = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean supplied = false;
        for (var target : targets) {
            IFluidHandler handler = target.cache.getCapability();
            if (handler == null || !handlers.add(handler)) continue;
            // Resolve proxy identity at operation time: a hatch can change its backing container without changing its own capability.
            BlockPos endpoint = target.cache.pos();
            if (!server.hasChunkAt(endpoint)) continue;
            if (server.getBlockEntity(endpoint) instanceof FluidHatchBlockEntity)
                endpoint = FluidHatchBlockEntity.resolveContainer(server, endpoint, target.cache.context());
            if (endpoint == null || !server.hasChunkAt(endpoint)) continue;
            var entity = server.getBlockEntity(endpoint);
            // Do not spend supply work on another infinite source or creative fluid sink.
            if (entity instanceof CopperWaterBasinBlockEntity || entity instanceof CreativeFluidTankBlockEntity
                || handler instanceof CreativeFluidTankBlockEntity.CreativeSmartFluidTank) continue;
            BlockPos identity = entity instanceof FluidTankBlockEntity tank ? tank.getController() : endpoint;
            if (controllers.contains(identity)) continue;
            if (supply(handler, budget, rate == -1)) { supplied = true; controllers.add(identity); }
        }
        nextSupplyTick = server.getGameTime() + (supplied ? ACTIVE_INTERVAL : IDLE_INTERVAL);
    }
    private static boolean supply(IFluidHandler handler, int budget, boolean unlimited) {
        FluidStack water = new FluidStack(Fluids.WATER, 1);
        long remaining = budget;
        if (unlimited) {
            remaining = 0;
            // Sum real slot room so even multi-tank handlers above the int limit can be filled in one operation.
            // This is a finite bound: accepting sinks cannot cause an endless fill-until-rejected loop.
            for (int slot = 0; slot < handler.getTanks(); slot++) {
                FluidStack stored = handler.getFluidInTank(slot);
                if ((stored.isEmpty() || FluidStack.isSameFluidSameComponents(stored, water)) && handler.isFluidValid(slot, water))
                    remaining += Math.max(0L, (long) handler.getTankCapacity(slot) - stored.getAmount());
            }
        }
        boolean supplied = false;
        while (remaining > 0) {
            int offered = (int) Math.min(Integer.MAX_VALUE, remaining);
            int accepts = Math.clamp(handler.fill(water.copyWithAmount(offered), FluidAction.SIMULATE), 0, offered);
            if (accepts == 0) break;
            int filled = Math.clamp(handler.fill(water.copyWithAmount(accepts), FluidAction.EXECUTE), 0, accepts);
            if (filled == 0) break;
            supplied = true;
            remaining -= filled;
            // Respect endpoints' own per-call limits; do not repeatedly hammer a throttled handler.
            if (!unlimited || filled < offered) break;
        }
        return supplied;
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        super.addToGoggleTooltip(tooltip, sneaking);
        int rate = CCDConfig.WATER_SUPPLY_RATE.get();
        tooltip.add(Component.translatable(rate == 0 ? "ccd.water_supply.disabled"
            : getBlockState().getValue(CopperWaterSupplyBlock.POWERED) ? "ccd.water_supply.paused" : "ccd.water_supply.active"));
        tooltip.add((rate == -1 ? Component.translatable("ccd.water_supply.unlimited")
            : Component.translatable("ccd.water_supply.rate", rate)).withStyle(ChatFormatting.GRAY));
        return true;
    }
}
