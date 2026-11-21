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
            try{radiation.invoke(null,entity,5d,60);return;}
            catch(ReflectiveOperationException exception){
                if(!radiationWarned){LOGGER.warn("Unable to apply Create Nuclear fluid radiation",exception);radiationWarned=true;}
                return;
            }
        }
        // Only apply an actual upstream effect; never invent a poison replacement for radiation.
        var effect=BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.fromNamespaceAndPath("createnuclear","radiation"));
        effect.ifPresent(holder->entity.addEffect(new MobEffectInstance(holder,60,0)));
    }
    private static void effect(LivingEntity entity,net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,int duration,int amplifier){
        entity.addEffect(new MobEffectInstance(effect,duration,amplifier));
    }
    public static void apply(LivingEntity entity,FluidType fluid,boolean submerged){
        if(entity.level().isClientSide()||!entity.isAlive()||entity.isSpectator()
            ||entity instanceof Player player&&player.isCreative())return;
        if(fluid==CCDFluids.QUICKSAND_TYPE.get()){
            // Keep vertical sinking possible, but resist lateral movement and upward escape.
            var velocity=entity.getDeltaMovement();
            entity.setDeltaMovement(velocity.x*.35,Math.min(velocity.y,0),velocity.z*.35);
            if(submerged&&entity.tickCount%20==0)entity.hurt(entity.damageSources().inWall(),2);
            return;
        }
        if(entity.tickCount%20!=0)return;
        for(var medium:ProcessingMedia.ALL)if(fluid==medium.type.get()){
            switch(medium.id){
                case "enriching_reagent" -> irradiate(entity);
                case "soul_stripping_reagent" -> {
                    effect(entity,MobEffects.WITHER,60,0);
                    effect(entity,MobEffects.WEAKNESS,60,0);
                }
                case "explosive_slurry" -> {
                    effect(entity,MobEffects.POISON,60,0);
                    // Volatile slurry flashes on contact with a burning entity, without a terrain explosion.
                    if(entity.isOnFire())entity.hurt(entity.damageSources().onFire(),4);
                }
                case "resonant_solution" -> {
                    effect(entity,MobEffects.GLOWING,100,0);
                    effect(entity,MobEffects.MOVEMENT_SLOWDOWN,60,0);
                }
                case "sculk_suspension" -> {
                    effect(entity,MobEffects.DARKNESS,100,0);
                    effect(entity,MobEffects.DIG_SLOWDOWN,60,0);
                }
                case "purifying_solution" -> {
                    // Remove harmful effects only; do not turn this fluid into a healing source.
                    for(var active:java.util.List.copyOf(entity.getActiveEffects()))
                        if(active.getEffect().value().getCategory()==net.minecraft.world.effect.MobEffectCategory.HARMFUL)
                            entity.removeEffect(active.getEffect());
                    entity.clearFire();
                }
                default -> {} // Super lava owns its native lava contact behavior.
            }
            return;
        }
    }
    @SubscribeEvent public static void contact(EntityTickEvent.Post event){
        if(!(event.getEntity() instanceof LivingEntity entity)||entity.level().isClientSide())return;
        // NeoForge updates these heights from the actual collision volume, including flowing levels.
        if(entity.getFluidTypeHeight(CCDFluids.QUICKSAND_TYPE.get())>0)
            apply(entity,CCDFluids.QUICKSAND_TYPE.get(),entity.isEyeInFluidType(CCDFluids.QUICKSAND_TYPE.get()));
        for(var medium:ProcessingMedia.ALL)if(!medium.id.equals("seething_reagent")&&entity.getFluidTypeHeight(medium.type.get())>0)
            apply(entity,medium.type.get(),entity.isEyeInFluidType(medium.type.get()));
    }
}
