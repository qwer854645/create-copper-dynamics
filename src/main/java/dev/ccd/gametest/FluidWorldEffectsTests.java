package dev.ccd.gametest;

import dev.ccd.compat.FluidWorldEffects;
import dev.ccd.compat.ProcessingMedia;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class FluidWorldEffectsTests {
    @GameTest(template="empty",timeoutTicks=150) public static void purificationHealsSlowlyAndExplosionPreservesBlocks(GameTestHelper h){
        var cow=h.spawn(net.minecraft.world.entity.EntityType.COW,2,2,2);cow.setNoAi(true);cow.setHealth(5);
        var pure=ProcessingMedia.ALL.stream().filter(m->m.id.equals("purifying_solution")).findFirst().orElseThrow();
        FluidWorldEffects.apply(cow,pure.type.get(),true);
        h.assertTrue(cow.getHealth()==5,"Purification healed immediately");
        h.runAfterDelay(100,()->{
            FluidWorldEffects.apply(cow,pure.type.get(),true);
            h.assertTrue(cow.getHealth()==6,"Expected half-heart healing after five seconds");
            var target=h.spawn(net.minecraft.world.entity.EntityType.COW,4,2,4);target.setNoAi(true);
            var pos=new net.minecraft.core.BlockPos(4,1,4);h.setBlock(pos,net.minecraft.world.level.block.Blocks.GLASS);
            target.igniteForSeconds(10);
            var slurry=ProcessingMedia.ALL.stream().filter(m->m.id.equals("explosive_slurry")).findFirst().orElseThrow();
            FluidWorldEffects.apply(target,slurry.type.get(),true);
            h.assertTrue(h.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.GLASS),"Explosion destroyed glass");
            h.assertTrue(target.getDeltaMovement().lengthSqr()>0,"Explosion did not knock back target");h.succeed();
        });
    }
    @GameTest(template="empty") public static void contactEffectsAndPurification(GameTestHelper h){
        var cow=h.spawn(net.minecraft.world.entity.EntityType.COW,2,2,2);cow.tickCount=20;
        cow.setDeltaMovement(1,.2,1);
        FluidWorldEffects.apply(cow,dev.ccd.CCDFluids.QUICKSAND_TYPE.get(),false);
        h.assertTrue(cow.getDeltaMovement().x<.4&&cow.getDeltaMovement().y==0,"Quicksand did not impede movement");
        cow.setDeltaMovement(1,.4,1);
        FluidWorldEffects.apply(cow,dev.ccd.CCDFluids.SLIME_TYPE.get(),false);
        h.assertTrue(cow.getDeltaMovement().y==.2&&cow.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN),"Slime contact effect missing");
        FluidWorldEffects.apply(cow,dev.ccd.CCDFluids.HAUNTING_TYPE.get(),false);
        h.assertTrue(cow.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS),"Soul fluid contact effect missing");
        for(var medium:ProcessingMedia.ALL){
            if(medium.id.equals("seething_reagent"))continue;
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
