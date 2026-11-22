package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.atomizing.AtomizerProcessing;
import dev.ccd.compat.FanCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class ThirdPartyOutputFilterTests {
    @GameTest(template="empty") public static void recipeRoutesRejectAndAcceptWithoutChangingInput(GameTestHelper h){
        var pos=new BlockPos(2,2,2);h.setBlock(pos,CopperDynamics.FLUID_ATOMIZER.get().defaultBlockState());
        var owner=(dev.ccd.content.FluidAtomizerBlockEntity)h.getBlockEntity(pos);
        var filter=owner.getBehaviour(com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour.TYPE);
        int checked=0;
        for(var namespace:java.util.List.of("dndesires","garnished","create_shimmer","create_dragons_plus")){
            if(!net.neoforged.fml.ModList.get().isLoaded(namespace))continue;
            for(var holder:h.getLevel().getRecipeManager().getRecipes()){
                var recipe=holder.value();var id=net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
                if(id==null||!id.getNamespace().equals(namespace)||recipe.getIngredients().isEmpty())continue;
                var type=FanCompat.find(namespace,id.getPath());if(type==null)continue;
                var stacks=recipe.getIngredients().getFirst().getItems();if(stacks.length==0)continue;
                var input=stacks[0].copyWithCount(3);if(!type.canProcess(input,h.getLevel()))continue;
                ItemStack output=recipe.getResultItem(h.getLevel().registryAccess());
                if(recipe instanceof com.simibubi.create.content.processing.recipe.ProcessingRecipe<?,?> processing)
                    output=processing.getRollableResults().stream().filter(r->r.getChance()>0&&!r.getStack().isEmpty()).map(r->r.getStack()).findFirst().orElse(ItemStack.EMPTY);
                if(output.isEmpty())continue;
                filter.setFilter(new ItemStack(Items.BARRIER));
                AtomizerProcessing.withSource(owner,()->{
                    var transported=new com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack(input.copy());
                    for(int i=0;i<200;i++)com.simibubi.create.content.kinetics.fan.processing.FanProcessing.applyProcessing(transported,h.getLevel(),type);
                    h.assertTrue(ItemStack.matches(transported.stack,input),"Filtered input changed: "+holder.id());
                });
                filter.setFilter(output);
                final boolean[] supported={false};
                AtomizerProcessing.withSource(owner,()->supported[0]=AtomizerProcessing.supportsOutputFilter(type,input,h.getLevel()));
                h.assertTrue(supported[0],"Known recipe route remains blocked: "+holder.id());checked++;
                AtomizerProcessing.withSource(owner,()->{
                    var results=type.process(input.copy(),h.getLevel());
                    h.assertTrue(results!=null,"Accepted recipe failed execution: "+holder.id());
                });
            }
        }
        if(Boolean.getBoolean("ccd.thirdPartyFilterProbe"))h.assertTrue(checked>=8,"Too few real recipe routes tested: "+checked);
        org.slf4j.LoggerFactory.getLogger("CCD-FilterTests").info("CCD_THIRD_PARTY_FILTER_ROUTES checked={}",checked);h.succeed();
    }
    @GameTest(template="empty") public static void dragonsAutomaticColoringUsesDeclaredPreview(GameTestHelper h){
        var type=FanCompat.find("create_dragons_plus","coloring_red");if(type==null){h.succeed();return;}
        var pos=new BlockPos(2,2,2);h.setBlock(pos,CopperDynamics.FLUID_ATOMIZER.get().defaultBlockState());
        var owner=(dev.ccd.content.FluidAtomizerBlockEntity)h.getBlockEntity(pos);
        var filter=owner.getBehaviour(com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour.TYPE);
        var input=new ItemStack(Items.WHITE_WOOL,4);
        filter.setFilter(new ItemStack(Items.RED_WOOL));
        AtomizerProcessing.withSource(owner,()->{
            h.assertTrue(AtomizerProcessing.supportsOutputFilter(type,input,h.getLevel()),"Red wool preview rejected");
            var result=type.process(input.copy(),h.getLevel());
            h.assertTrue(result!=null&&result.stream().anyMatch(s->s.is(Items.RED_WOOL)),"Coloring did not yield red wool");
        });
        filter.setFilter(new ItemStack(Items.DIAMOND));
        AtomizerProcessing.withSource(owner,()->{
            h.assertTrue(!AtomizerProcessing.supportsOutputFilter(type,input,h.getLevel()),"Wrong coloring target accepted");
            var dropped=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),0,0,0,input.copy());
            for(int i=0;i<200;i++)com.simibubi.create.content.kinetics.fan.processing.FanProcessing.applyProcessing(dropped,type);
            h.assertTrue(ItemStack.matches(dropped.getItem(),input),"Rejected dropped item changed");
        });h.succeed();
    }
}
