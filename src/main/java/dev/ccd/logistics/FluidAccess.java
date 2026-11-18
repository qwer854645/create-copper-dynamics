package dev.ccd.logistics;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import dev.ccd.content.FluidAccessBlock;
import dev.ccd.content.FluidAccessBlockEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Bounded, sided resolution; forwarding blocks never own a copy of their source fluids. */
public final class FluidAccess {
    public record Source(BlockPos identity, IFluidHandler handler, Predicate<FluidStack> filter) {}
    private static final ThreadLocal<Set<BlockPos>> ACTIVE = ThreadLocal.withInitial(HashSet::new);
    public static boolean hasDirectContainer(Level level, BlockPos origin, Direction end) {
        BlockPos target = origin.relative(end);
        return level.hasChunkAt(target) && !(level.getBlockEntity(target) instanceof FluidAccessBlockEntity)
            && level.getCapability(Capabilities.FluidHandler.BLOCK, target, end.getOpposite()) != null;
    }
    public static List<Source> resolve(Level level, BlockPos pos, Direction side) {
        return resolve(level, pos, side, new HashSet<>(), 0);
    }
    private static List<Source> resolve(Level level, BlockPos pos, Direction side, Set<BlockPos> visited, int depth) {
        if (level == null || depth > 16 || !level.hasChunkAt(pos) || !visited.add(pos)) return List.of();
        var entity = level.getBlockEntity(pos);
        if (entity instanceof FluidAccessBlockEntity access) {
            if (depth != 0 || level.hasNeighborSignal(pos)) return List.of();
            var state = access.getBlockState();
            var block = (FluidAccessBlock) state.getBlock();
            var facing = state.getValue(FluidAccessBlock.FACING);
            if (block.isBridge()) {
                var result = new ArrayList<Source>();
                Direction negative = Direction.fromAxisAndDirection(facing.getAxis(), Direction.AxisDirection.NEGATIVE);
                for (var end : List.of(negative, negative.getOpposite())) {
                    for (var source : resolve(level, pos.relative(end), end.getOpposite(), new HashSet<>(visited), depth+1))
                        result.add(new Source(source.identity(), source.handler(), fluid -> source.filter().test(fluid) && access.accepts(end, fluid)));
                }
                // Prefer the first sided view of an identical native controller.
                // Do not sum the same physical tanks through two proxy entrances.
                var identities = new HashSet<BlockPos>();
                return result.stream().filter(source -> identities.add(source.identity())).toList();
            }
            var attached = pos.relative(facing.getOpposite());
            if (!level.hasChunkAt(attached) || level.getBlockEntity(attached) instanceof FluidAccessBlockEntity) return List.of();
            return resolve(level, pos.relative(facing.getOpposite()), facing, visited, depth+1);
        }
        var active = ACTIVE.get();
        if (active.size() >= 32 || !active.add(pos)) return List.of();
        try {
            var handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
            if (handler == null) return List.of();
            return List.of(new Source(entity instanceof FluidTankBlockEntity tank ? tank.getController() : pos.immutable(), handler, fluid -> true));
        } finally { active.remove(pos); if (active.isEmpty()) ACTIVE.remove(); }
    }

    public static IFluidHandler handler(java.util.function.Supplier<List<Source>> supplier) {
        return new IFluidHandler() {
            private final ThreadLocal<Boolean> active = ThreadLocal.withInitial(() -> false);
            private List<Source> sources() { return supplier.get(); }
            private Source slot(List<Source> sources, int tank) {
                if (tank < 0) return null;
                for (var source : sources) { if (tank < source.handler().getTanks()) return source; tank -= source.handler().getTanks(); }
                return null;
            }
            private int local(List<Source> sources, Source target, int tank) {
                for (var source : sources) { if (source == target) break; tank -= source.handler().getTanks(); }
                return tank;
            }
            @Override public int getTanks() { return sources().stream().mapToInt(s -> s.handler().getTanks()).sum(); }
            @Override public FluidStack getFluidInTank(int tank) {
                var sources = sources(); var source = slot(sources,tank);
                if (source == null) return FluidStack.EMPTY;
                var fluid = source.handler().getFluidInTank(local(sources,source,tank)).copy();
                return source.filter().test(fluid) ? fluid : FluidStack.EMPTY;
            }
            @Override public int getTankCapacity(int tank) { var sources=sources(); var s=slot(sources,tank); return s==null?0:s.handler().getTankCapacity(local(sources,s,tank)); }
            @Override public boolean isFluidValid(int tank, FluidStack fluid) { var sources=sources(); var s=slot(sources,tank); return s!=null && s.filter().test(fluid) && s.handler().isFluidValid(local(sources,s,tank),fluid); }
            @Override public int fill(FluidStack fluid, FluidAction action) {
                if (fluid.isEmpty() || active.get()) return 0;
                active.set(true);
                try {
                    int remaining=fluid.getAmount(); var used=new HashSet<BlockPos>(); var sources=sources();
                    for (var s:sources) {
                        if (remaining<=0 || used.contains(s.identity()) || !s.filter().test(fluid)) continue;
                        int filled=s.handler().fill(fluid.copyWithAmount(remaining),action);
                        // Visit each source once even when an active endpoint changes
                        // its displayed contents between simulation and execution.
                        used.add(s.identity());
                        if(filled>0) remaining-=Math.min(remaining,filled);
                    }
                    return fluid.getAmount()-remaining;
                } finally { active.remove(); }
            }
            @Override public FluidStack drain(FluidStack wanted, FluidAction action) {
                if(wanted.isEmpty() || active.get()) return FluidStack.EMPTY;
                active.set(true);
                try {
                    int remaining=wanted.getAmount(); var used=new HashSet<BlockPos>();
                    for(var s:sources()) {
                        if(remaining<=0 || used.contains(s.identity()) || !s.filter().test(wanted)) continue;
                        var drained=s.handler().drain(wanted.copyWithAmount(remaining),action);
                        if(!drained.isEmpty() && FluidStack.isSameFluidSameComponents(wanted,drained)) { remaining-=Math.min(remaining,drained.getAmount()); used.add(s.identity()); }
                    }
                    return remaining==wanted.getAmount()?FluidStack.EMPTY:wanted.copyWithAmount(wanted.getAmount()-remaining);
                } finally { active.remove(); }
            }
            @Override public FluidStack drain(int amount, FluidAction action) {
                if(amount<=0 || active.get()) return FluidStack.EMPTY;
                for(var s:sources()) for(int i=0;i<s.handler().getTanks();i++) {
                    var fluid=s.handler().getFluidInTank(i);
                    if(fluid.isEmpty() || !s.filter().test(fluid)) continue;
                    var result=drain(fluid.copyWithAmount(amount),action);
                    if(!result.isEmpty()) return result;
                }
                return FluidStack.EMPTY;
            }
        };
    }
}
