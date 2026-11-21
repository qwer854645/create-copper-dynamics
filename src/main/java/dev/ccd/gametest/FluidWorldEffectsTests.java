package dev.ccd.gametest;

import dev.ccd.compat.FluidWorldEffects;
import dev.ccd.compat.ProcessingMedia;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class FluidWorldEffectsTests {
    @GameTest(template="empty") public static void contactEffectsAndPurification(GameTestHelper h){
        var cow=h.spawn(net.minecraft.world.entity.EntityType.COW,2,2,2);cow.tickCount=20;
        cow.setDeltaMovement(1,.2,1);
        FluidWorldEffects.apply(cow,dev.ccd.CCDFluids.QUICKSAND_TYPE.get(),false);
        h.assertTrue(cow.getDeltaMovement().x<.4&&cow.getDeltaMovement().y==0,"Quicksand did not impede movement");
        for(var medium:ProcessingMedia.ALL){
            FluidWorldEffects.apply(cow,medium.type.get(),false);
            switch(medium.id){
                case "soul_stripping_reagent" -> h.assertTrue(cow.hasEffect(net.minecraft.world.effect.MobEffects.WITHER),"Soul fluid effect missing");
                case "explosive_slurry" -> h.assertTrue(cow.hasEffect(net.minecraft.world.effect.MobEffects.POISON),"Slurry effect missing");
                case "resonant_solution" -> h.assertTrue(cow.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING),"Resonance effect missing");
                case "sculk_suspension" -> h.assertTrue(cow.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS),"Sculk effect missing");
                case "purifying_solution" -> h.assertTrue(cow.getActiveEffects().stream().noneMatch(e->e.getEffect().value().getCategory()==net.minecraft.world.effect.MobEffectCategory.HARMFUL),"Purification did not remove harmful effects");
            }
        }
        h.succeed();
    }
}
