package dev.ccd.content;

import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

public final class FlowMeterBlockEntity extends FluidPipeBlockEntity implements IHaveGoggleInformation {
    private long sampleTick=Long.MIN_VALUE;
    private int accumulated;
    private int previousAccumulated;
    private long previousSampleTick=Long.MIN_VALUE;
    public int flowRate;
    public float previousDial,dial;
    public FlowMeterBlockEntity(BlockPos pos,BlockState state){super(CopperDynamics.FLOW_METER_BE.get(),pos,state);}
    public static void recordPath(net.minecraft.world.level.Level level,BlockPos end,java.util.Map<BlockPos,BlockPos> parents,int amount){
        if(amount<=0)return;
        for(var pos=end;pos!=null;pos=parents.get(pos)){
            if(level.getBlockEntity(pos) instanceof FlowMeterBlockEntity meter)meter.recordTransfer(amount);
            if(pos.equals(parents.get(pos)))break;
        }
    }
    public void recordTransfer(int amount){
        if(!dev.ccd.CCDFeatures.enabled(getBlockState()))return;
        if(level==null||level.isClientSide()||amount<=0)return;
        long tick=level.getGameTime();
        if(sampleTick!=tick){previousSampleTick=sampleTick;previousAccumulated=accumulated;sampleTick=tick;accumulated=0;}
        accumulated=(int)Math.min(Integer.MAX_VALUE,(long)accumulated+amount);
    }
    @Override public void tick(){
        if (!dev.ccd.CCDFeatures.enabled(getBlockState())) return;
        super.tick();if(level==null)return;
        if(level.isClientSide()){
            previousDial=dial;
            float target=(float)(Math.log1p(flowRate)/Math.log(10001));
            dial+=(Math.clamp(target,0,1)-dial)*.2f;return;
        }
        // Read the previous tick, independent of block entity/network ticking order.
        long prior=level.getGameTime()-1;
        int next=sampleTick==prior?accumulated:previousSampleTick==prior?previousAccumulated:0;
        if(next!=flowRate){flowRate=next;sendData();}
    }
    @Override protected void write(CompoundTag tag,HolderLookup.Provider registries,boolean packet){super.write(tag,registries,packet);if(packet)tag.putInt("FlowRate",flowRate);}
    @Override protected void read(CompoundTag tag,HolderLookup.Provider registries,boolean packet){super.read(tag,registries,packet);if(packet)flowRate=tag.getInt("FlowRate");}
    @Override public boolean addToGoggleTooltip(java.util.List<Component> tooltip,boolean sneaking){
        dev.ccd.CCDTooltip.heading(tooltip,"ccd.goggles.flow");
        com.simibubi.create.foundation.utility.CreateLang.builder().add(dev.ccd.CCDTooltip.number(flowRate))
            .add(Component.translatable("ccd.unit.flow").withStyle(net.minecraft.ChatFormatting.AQUA)).forGoggles(tooltip,1);return true;
    }
}
