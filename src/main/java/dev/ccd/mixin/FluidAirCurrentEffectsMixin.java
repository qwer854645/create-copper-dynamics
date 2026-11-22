package dev.ccd.mixin;

import com.simibubi.create.content.kinetics.fan.AirCurrent;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Additional entity effects only: never replaces the selected recipe processing type. */
@Mixin(AirCurrent.class)
abstract class FluidAirCurrentEffectsMixin {
    @Shadow protected java.util.List<net.minecraft.world.entity.Entity> caughtEntities;
    @Inject(method="tickAffectedEntities",at=@At("TAIL"))
    private void ccd$fluidExposure(Level level,CallbackInfo ci){
        if(level==null||level.isClientSide())return;
        var air=(AirCurrent)(Object)this;
        if(air.source instanceof dev.ccd.content.FluidAtomizerBlockEntity||air.direction==null)return;
        var origin=net.minecraft.world.phys.Vec3.atCenterOf(air.source.getAirCurrentPos());
        for(var entity:caughtEntities)if(entity instanceof net.minecraft.world.entity.LivingEntity living&&living.isAlive()&&living.getBoundingBox().intersects(air.bounds)){
            double distance=entity.position().subtract(origin).dot(net.minecraft.world.phys.Vec3.atLowerCornerOf(air.direction.getNormal()));
            var types=new java.util.HashSet<net.neoforged.neoforge.fluids.FluidType>();
            for(int i=1;i<=Math.ceil(air.maxDistance);i++){
                if(air.pushing?i-.5>distance:i+.5<distance)continue;
                var pos=air.source.getAirCurrentPos().relative(air.direction,i);
                if(!level.hasChunkAt(pos))continue;
                var state=level.getFluidState(pos);if(state.isEmpty())continue;
                var type=state.getFluidType();
                if(types.add(type))dev.ccd.compat.FluidWorldEffects.apply(living,type,true);
            }
        }
    }
}
