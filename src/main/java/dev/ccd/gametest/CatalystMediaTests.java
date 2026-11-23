package dev.ccd.gametest;

import dev.ccd.compat.ProcessingMedia;
import dev.ccd.compat.FanCompat;
import dev.ccd.compat.FluidWorldEffects;
import dev.ccd.atomizing.AtomizingRules;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class CatalystMediaTests {
    @GameTest(template="empty") public static void smokingFluidUsesNativeRecipes(GameTestHelper h) {
        var medium=ProcessingMedia.ALL.stream().filter(m->m.id.equals("smoking_fluid")).findFirst().orElseThrow();
        var smoking=com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes.SMOKING;
        for(var fluid:java.util.List.of(medium.source.get(),medium.flowing.get())) {
            var selected=AtomizingRules.select(h.getLevel(),new FluidStack(fluid,1000));
            h.assertTrue(selected!=null&&selected.type()==smoking,"Atomizer did not select native smoking");
        }
        var pos=new BlockPos(2,2,2);h.setBlock(pos,medium.block.get());
        h.assertTrue(com.simibubi.create.content.kinetics.fan.processing.FanProcessingType.getAt(h.getLevel(),h.absolutePos(pos))==smoking,"World fluid did not enable native smoking");
        var input=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BEEF,3);
        var output=smoking.process(input.copy(),h.getLevel());
        h.assertTrue(output!=null&&output.stream().filter(s->s.is(net.minecraft.world.item.Items.COOKED_BEEF)).mapToInt(net.minecraft.world.item.ItemStack::getCount).sum()==3,"Native smoking recipe changed");
        h.assertTrue(medium.available(),"Base medium incorrectly requires optional mod");
        h.assertTrue(h.getLevel().getRecipeManager().getRecipes().stream().anyMatch(r->r.id().toString().equals("ccd:mixing/smoking_fluid")),"Missing production recipe");
        h.succeed();
    }
    @GameTest(template="empty") public static void newMediaTagsAndProcessing(GameTestHelper h) {
        var mappings=java.util.Map.of("withering_fluid","withering","conduit_energy_fluid","tidal","moss_suspension","mossify","tear_solution","tear","corrupting_fluid","corrupted");
        var pos=new BlockPos(2,2,2);
        for(var entry:mappings.entrySet()) {
            var medium=ProcessingMedia.ALL.stream().filter(m->m.id.equals(entry.getKey())).findFirst().orElseThrow();
            for(var fluid:java.util.List.of(medium.source.get(),medium.flowing.get()))
                h.assertTrue(FanCompat.tagged(new FluidStack(fluid,1000),"create_more_catalysts","fan_catalysts/"+entry.getValue()),"Missing fluid tag: "+entry.getKey());
            var expected=FanCompat.find("create_more_catalysts",entry.getValue());
            var selection=AtomizingRules.select(h.getLevel(),new FluidStack(medium.source.get(),1000));
            h.assertTrue(selection!=null,"No atomizer action for "+medium.id);
            if(expected!=null) {
                h.assertTrue(selection.type()==expected,"Wrong atomizer type: "+medium.id);
                h.setBlock(pos,medium.block.get());
                h.assertTrue(com.simibubi.create.content.kinetics.fan.processing.FanProcessingType.getAt(h.getLevel(),h.absolutePos(pos))==expected,"Wrong fan type: "+medium.id);
            }
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void newMediaMistStrongerAndShorter(GameTestHelper h) {
        var effects=java.util.Map.<String,net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>>of("withering_fluid",MobEffects.WITHER,"conduit_energy_fluid",MobEffects.DIG_SPEED,"moss_suspension",dev.ccd.CopperDynamics.MOSSIFICATION,"tear_solution",MobEffects.WEAKNESS,"corrupting_fluid",MobEffects.WITHER);
        for(var entry:effects.entrySet()) {
            var medium=ProcessingMedia.ALL.stream().filter(m->m.id.equals(entry.getKey())).findFirst().orElseThrow();
            var normal=h.spawn(net.minecraft.world.entity.EntityType.COW,2,2,2);
            var mist=h.spawn(net.minecraft.world.entity.EntityType.COW,4,2,2);
            FluidWorldEffects.apply(normal,medium.type.get(),true);FluidWorldEffects.applyMist(mist,medium.type.get());
            var weak=normal.getEffect(entry.getValue());var strong=mist.getEffect(entry.getValue());
            h.assertTrue(weak!=null&&strong!=null&&strong.getAmplifier()>weak.getAmplifier()&&strong.getDuration()<weak.getDuration(),"Mist scaling missing: "+medium.id);
            normal.discard();mist.discard();
        }
        h.succeed();
    }
}
