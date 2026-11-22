package dev.ccd.compat;

import dev.ccd.CCDFluids;
import dev.ccd.CopperDynamics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.fluids.FluidType;

/** Contact-only effects: storage, pipes and held buckets do not expose entities. */
@EventBusSubscriber(modid=CopperDynamics.ID)
public final class FluidWorldEffects {
    private FluidWorldEffects(){}
    private static boolean radiationResolved;
    private static java.lang.reflect.Method radiation;
    private static final org.slf4j.Logger LOGGER=org.slf4j.LoggerFactory.getLogger(FluidWorldEffects.class);
    private static boolean radiationWarned;
    public static void irradiate(LivingEntity entity){
        irradiate(entity,false);
    }
    private static void irradiate(LivingEntity entity,boolean mist){
        if(!ModList.get().isLoaded("createnuclear"))return;
        if(!radiationResolved){
            radiationResolved=true;
            try{
                var type=Class.forName("net.nuclearteam.createnuclear.content.radiation.capability.RadiationCapability");
                radiation=type.getMethod("applyContagion",LivingEntity.class,double.class,int.class);
            }catch(ReflectiveOperationException ignored){
                // Older Nuclear versions expose a registered effect instead of this API.
            }
        }
        if(radiation!=null){
            try{radiation.invoke(null,entity,mist?10d:5d,mist?30:60);return;}
            catch(ReflectiveOperationException exception){
                if(!radiationWarned){LOGGER.warn("Unable to apply Create Nuclear fluid radiation",exception);radiationWarned=true;}
                return;
            }
        }
        // Only apply an actual upstream effect; never invent a poison replacement for radiation.
        var effect=BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.fromNamespaceAndPath("createnuclear","radiation"));
        effect.ifPresent(holder->entity.addEffect(new MobEffectInstance(holder,mist?30:60,mist?2:0)));
    }
    private static void effect(LivingEntity entity,net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,int duration,int amplifier){
        entity.addEffect(new MobEffectInstance(effect,duration,amplifier));
    }
    public static void apply(LivingEntity entity,FluidType fluid,boolean submerged){
        apply(entity,fluid,submerged,false);
    }
    public static void applyMist(LivingEntity entity,FluidType fluid){apply(entity,fluid,true,true);}
    private static void mistEffect(LivingEntity entity,net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> type,int duration,int amplifier,boolean mist){
        boolean scales=type!=MobEffects.GLOWING&&type!=MobEffects.DARKNESS;
        effect(entity,type,mist?Math.max(20,duration/2):duration,amplifier+(mist&&scales?2:0));
    }
    private static void apply(LivingEntity entity,FluidType fluid,boolean submerged,boolean mist){
        if(entity.level().isClientSide()||!entity.isAlive()||entity.isSpectator()
            ||entity instanceof Player player&&player.isCreative())return;
        String stamp="ccdFluidEffect/"+fluid.getDescriptionId()+(mist?"/mist":"");
        long now=entity.level().getGameTime();
        if(entity.getPersistentData().contains(stamp)&&entity.getPersistentData().getLong(stamp)==now)return;
        entity.getPersistentData().putLong(stamp,now);
        if(fluid==CCDFluids.QUICKSAND_TYPE.get()){
            // Keep vertical sinking possible, but resist lateral movement and upward escape.
            var velocity=entity.getDeltaMovement();
            entity.setDeltaMovement(velocity.x*(mist?.2:.35),Math.min(velocity.y,0),velocity.z*(mist?.2:.35));
            if(submerged&&pulse(entity,stamp,now))entity.hurt(entity.damageSources().inWall(),mist?4:2);
            return;
        }
        if(fluid==CCDFluids.SLIME_TYPE.get()){
            var velocity=entity.getDeltaMovement();
            entity.setDeltaMovement(velocity.x*(mist?.35:.6),velocity.y>0?velocity.y*(mist?.25:.5):velocity.y,velocity.z*(mist?.35:.6));
            if(pulse(entity,stamp,now))mistEffect(entity,MobEffects.MOVEMENT_SLOWDOWN,60,1,mist);
            return;
        }
        if(!pulse(entity,stamp,now))return;
        if(fluid==ProcessingMedia.ALL.getFirst().type.get()){
            entity.igniteForSeconds(mist?5:15);
            if(!entity.fireImmune())entity.hurt(entity.damageSources().lava(),mist?30:20);
            return;
        }
        if(fluid==CCDFluids.HAUNTING_TYPE.get()){
            mistEffect(entity,MobEffects.MOVEMENT_SLOWDOWN,60,0,mist);
            mistEffect(entity,MobEffects.WEAKNESS,60,0,mist);
            return;
        }
        for(var medium:ProcessingMedia.ALL)if(fluid==medium.type.get()){
            switch(medium.id){
                case "enriching_reagent" -> irradiate(entity,mist);
                case "soul_stripping_reagent" -> {
                    mistEffect(entity,MobEffects.WITHER,60,0,mist);
                    mistEffect(entity,MobEffects.WEAKNESS,60,0,mist);
                }
                case "explosive_slurry" -> {
                    mistEffect(entity,MobEffects.POISON,60,0,mist);
                    // Volatile slurry flashes on contact with a burning entity, without a terrain explosion.
                    if(entity.isOnFire()){
                        String key=mist?"ccdSlurryMistExplosion":"ccdSlurryExplosion";
                        if(!entity.getPersistentData().contains(key)||now-entity.getPersistentData().getLong(key)>=40){
                            entity.getPersistentData().putLong(key,now);
                            entity.level().explode(null,entity.getX(),entity.getY()-.1,entity.getZ(),mist?1.5f:1.0f,false,net.minecraft.world.level.Level.ExplosionInteraction.NONE);
                            // The triggering entity can shield the blast ray at its own center.
                            // Give it an explicit upward impulse as well as the radial blast.
                            entity.push(0,mist?.65:.45,0);
                            entity.hurtMarked=true;
                        }
                    }
                }
                case "resonant_solution" -> {
                    mistEffect(entity,MobEffects.GLOWING,100,0,mist);
                    mistEffect(entity,MobEffects.MOVEMENT_SLOWDOWN,60,0,mist);
                }
                case "sculk_suspension" -> {
                    mistEffect(entity,MobEffects.DARKNESS,100,0,mist);
                    mistEffect(entity,MobEffects.DIG_SLOWDOWN,60,0,mist);
                }
                case "purifying_solution" -> {
                    // Purification is immediate; healing is deliberately slow.
                    for(var active:java.util.List.copyOf(entity.getActiveEffects()))
                        if(active.getEffect().value().getCategory()==net.minecraft.world.effect.MobEffectCategory.HARMFUL)
                            entity.removeEffect(active.getEffect());
                    entity.clearFire();
                    String healed=mist?"ccdPurificationMistHeal":"ccdPurificationHeal";
                    if(!entity.getPersistentData().contains(healed)){entity.getPersistentData().putLong(healed,now);}
                    else if(now-entity.getPersistentData().getLong(healed)>=(mist?40:100)){entity.heal(mist?2:1);entity.getPersistentData().putLong(healed,now);}
                }
                default -> {} // Super lava owns its native lava contact behavior.
            }
            return;
        }
    }
    private static boolean pulse(LivingEntity entity,String stamp,long now){
        String key=stamp+"/pulse";
        if(entity.getPersistentData().contains(key)&&now-entity.getPersistentData().getLong(key)<20)return false;
        entity.getPersistentData().putLong(key,now);return true;
    }
    @SubscribeEvent public static void contact(EntityTickEvent.Post event){
        if(!(event.getEntity() instanceof LivingEntity entity)||entity.level().isClientSide())return;
        // NeoForge updates these heights from the actual collision volume, including flowing levels.
        if(entity.getFluidTypeHeight(CCDFluids.QUICKSAND_TYPE.get())>0)
            apply(entity,CCDFluids.QUICKSAND_TYPE.get(),entity.isEyeInFluidType(CCDFluids.QUICKSAND_TYPE.get()));
        for(var fluid:java.util.List.of(CCDFluids.SLIME_TYPE.get(),CCDFluids.HAUNTING_TYPE.get()))
            if(entity.getFluidTypeHeight(fluid)>0)apply(entity,fluid,entity.isEyeInFluidType(fluid));
        for(var medium:ProcessingMedia.ALL)if(!medium.id.equals("seething_reagent")&&entity.getFluidTypeHeight(medium.type.get())>0)
            apply(entity,medium.type.get(),entity.isEyeInFluidType(medium.type.get()));
    }
}
