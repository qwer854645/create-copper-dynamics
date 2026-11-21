package dev.ccd.logistics;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.OpenEndedPipe;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Persistent native open ends for devices that deliver from their own storage. */
public final class PumpOpenEnds {
    private final java.util.Map<BlockFace,OpenEndedPipe> ends=new java.util.HashMap<>();
    public IFluidHandler handler(BlockEntity owner,BlockPos output,Direction entry){
        var level=owner.getLevel();
        var face=new BlockFace(output.relative(entry),entry.getOpposite());
        if(level==null||!level.hasChunkAt(output)||!FluidPropagator.isOpenEnd(level,face.getPos(),face.getFace()))return null;
        var end=ends.computeIfAbsent(face,OpenEndedPipe::new);
        end.manageSource(level,owner);
        return end.provideHandler().getCapability();
    }
    public void write(CompoundTag tag,HolderLookup.Provider registries){
        var list=new ListTag();for(var end:ends.values())list.add(end.serializeNBT(registries));tag.put("PumpOpenEnds",list);
    }
    public void read(CompoundTag tag,HolderLookup.Provider registries){
        ends.clear();var list=tag.getList("PumpOpenEnds",Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++){
            var data=list.getCompound(i);var face=BlockFace.fromNBT(data.getCompound("Location"));
            ends.put(face,OpenEndedPipe.fromNBT(data,registries,face.getPos()));
        }
    }
}
