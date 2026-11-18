package dev.ccd.logistics;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import dev.ccd.content.FluidPackagerBlockEntity;
import dev.ccd.content.FluidStockItem;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

/** Count each physical fluid once per native logistics traversal, including overlapping bridges. */
public final class FluidStockScope implements AutoCloseable {
    private static final ThreadLocal<FluidStockScope> CURRENT = new ThreadLocal<>();
    private record Stock(ResourceKey<Level> dimension, BlockPos pos, net.minecraft.world.level.material.Fluid fluid,
        net.minecraft.core.component.DataComponentPatch components) {}
    private final FluidStockScope previous;
    private final Map<Stock,Integer> seen = new HashMap<>();
    private final Map<FluidPackagerBlockEntity,InventorySummary> summaries = new java.util.IdentityHashMap<>();
    private final IdentifiedInventory excluded;
    public FluidStockScope(IdentifiedInventory excluded) {
        previous=CURRENT.get(); this.excluded=excluded; CURRENT.set(this);
    }
    @Override public void close() { if(previous==null) CURRENT.remove(); else CURRENT.set(previous); }
    public static InventorySummary summarize(FluidPackagerBlockEntity packager) {
        var scope=CURRENT.get(); if(scope==null) return null;
        var cached=scope.summaries.get(packager);
        if(cached!=null) return cached;
        var level=packager.getLevel(); var summary=new InventorySummary();
        if(level==null) return summary;
        Set<BlockPos> excluded=scope.excluded!=null && scope.excluded.identifier() instanceof FluidInventoryBehaviour.AccessId id
            && id.dimension().equals(level.dimension()) ? id.sources() : Set.of();
        var sources=FluidAccess.resolve(level,packager.targetFluidPos(),packager.getBlockState().getValue(
            com.simibubi.create.content.logistics.packager.PackagerBlock.FACING));
        // Aggregate slots within a source before applying traversal-level overlap suppression.
        var current=new HashMap<Stock,Integer>(); var samples=new HashMap<Stock,FluidStack>();
        for(var source:sources) {
            if(excluded.contains(source.identity())) continue;
            var endpoint=new HashMap<Stock,Integer>();
            for(int i=0;i<source.handler().getTanks();i++) {
                var fluid=source.handler().getFluidInTank(i);
                if(fluid.isEmpty() || !source.filter().test(fluid)) continue;
                var key=new Stock(level.dimension(),source.identity(),fluid.getFluid(),fluid.getComponentsPatch());
                endpoint.merge(key,fluid.getAmount(),(a,b)->(int)Math.min(BigItemStack.INF,(long)a+b));
                samples.put(key,fluid.copy());
            }
            endpoint.forEach((key,amount)->current.merge(key,amount,Math::max));
        }
        current.forEach((key,amount)->{
            int previous=scope.seen.getOrDefault(key,0);
            scope.seen.merge(key,amount,Math::max);
            if(amount>previous) summary.add(FluidStockItem.of(samples.get(key)),amount-previous);
        });
        scope.summaries.put(packager,summary);
        return summary;
    }
}
