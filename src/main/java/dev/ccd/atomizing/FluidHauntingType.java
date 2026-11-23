package dev.ccd.atomizing;

import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;

/** Fluid catalyst for Create's live haunting recipes, including datapack additions. */
public final class FluidHauntingType implements FanProcessingType {
    @Override public boolean isValidAt(Level level,BlockPos pos){return dev.ccd.CCDFeatures.enabled("haunting")&&level.getFluidState(pos).getFluidType()==dev.ccd.CCDFluids.HAUNTING_TYPE.get();}
    @Override public int getPriority(){return AllFanProcessingTypes.HAUNTING.getPriority();}
    @Override public boolean canProcess(ItemStack stack,Level level){return AllFanProcessingTypes.HAUNTING.canProcess(stack,level);}
    @Override public java.util.List<ItemStack> process(ItemStack stack,Level level){return AllFanProcessingTypes.HAUNTING.process(stack,level);}
    @Override public void spawnProcessingParticles(Level level,net.minecraft.world.phys.Vec3 pos){AllFanProcessingTypes.HAUNTING.spawnProcessingParticles(level,pos);}
    @Override public void morphAirFlow(AirFlowParticleAccess particle,net.minecraft.util.RandomSource random){AllFanProcessingTypes.HAUNTING.morphAirFlow(particle,random);}
    @Override public void affectEntity(net.minecraft.world.entity.Entity entity,Level level){AllFanProcessingTypes.HAUNTING.affectEntity(entity,level);}
}
