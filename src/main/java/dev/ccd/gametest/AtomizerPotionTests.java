package dev.ccd.gametest;

import dev.ccd.content.FluidAtomizerBlockEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class AtomizerPotionTests {
    @GameTest(template="empty") public static void mistShortensPotionDuration(GameTestHelper h){
        var shortEffect=new MobEffectInstance(MobEffects.MOVEMENT_SPEED,100,1);
        var mist=FluidAtomizerBlockEntity.mistPotionEffect(shortEffect);
        h.assertTrue(mist.getDuration()==25&&mist.getAmplifier()==1,"Mist did not quarter duration or preserve level");
        h.assertTrue(FluidAtomizerBlockEntity.mistPotionEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,9600)).getDuration()==600,"Mist exceeds thirty seconds");
        h.assertTrue(FluidAtomizerBlockEntity.mistPotionEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,-1)).getDuration()==600,"Infinite potion was not bounded");
        h.succeed();
    }
}
