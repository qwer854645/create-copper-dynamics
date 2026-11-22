package dev.ccd.logistics;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/** One fixed endpoint view; NeoForge invalidates the cached handler when its capability changes. */
public final class AdjacentFluidCapability {
    private final SmartBlockEntity owner;
    private BlockCapabilityCache<IFluidHandler,Direction> cache;
    private long generation;
    public AdjacentFluidCapability(SmartBlockEntity owner){this.owner=owner;}
    @Nullable public IFluidHandler get(BlockPos target,Direction face){
        var level=owner.getLevel();
        if(level==null||owner.isRemoved()||!level.hasChunkAt(target))return null;
        if(!(level instanceof ServerLevel server))return level.getCapability(Capabilities.FluidHandler.BLOCK,target,face);
        if(cache==null||cache.level()!=server||!cache.pos().equals(target)||cache.context()!=face){
            long current=++generation;
            cache=BlockCapabilityCache.create(Capabilities.FluidHandler.BLOCK,server,target.immutable(),face,
                ()->current==generation&&!owner.isRemoved()&&owner.getLevel()==server,()->{});
        }
        return cache.getCapability();
    }
    public void clear(){generation++;cache=null;}
}
