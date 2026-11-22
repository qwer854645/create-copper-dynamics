package dev.ccd.gametest;

import dev.ccd.compat.FanCompat;
import dev.ccd.atomizing.AtomizingRules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class IndustrialCatalystTests {
    @GameTest(template="empty") public static void industrialFluidsSelectRealProcessingTypes(GameTestHelper h){
        String[][] cases={
            {"immersiveengineering","creosote","preservation"},
            {"mekanism","brine","salout"},{"mekanism","hydrofluoric_acid","etch"},
            {"mekanism","sulfur_dioxide","sulfon"},{"mekanism","sulfuric_acid","dehyd"},
            {"mekanism","oxygen","oxidize"},{"mekanism","hydrogen","reduce"},
            {"mekanismgenerators","bioethanol","ferment"}};
        if(!ModList.get().isLoaded("create_more_catalysts")){h.succeed();return;}
        for(var test:cases){
            if(!ModList.get().isLoaded(test[0]))continue;
            var type=FanCompat.find("create_more_catalysts",test[2]);
            h.assertTrue(type!=null,"Missing registered industrial type: "+test[2]);
            var id=ResourceLocation.fromNamespaceAndPath(test[0],test[1]);
            h.assertTrue(BuiltInRegistries.FLUID.containsKey(id),"Missing industrial fluid: "+id);
            for(var name:java.util.List.of(test[1],"flowing_"+test[1])){
                var key=ResourceLocation.fromNamespaceAndPath(test[0],name);
                if(!BuiltInRegistries.FLUID.containsKey(key))continue;
                var stack=new FluidStack(BuiltInRegistries.FLUID.get(key),1000);
                h.assertTrue(FanCompat.select(stack)==type,"Incorrect fluid selection: "+key);
                var plan=AtomizingRules.select(h.getLevel(),stack);
                h.assertTrue(plan!=null&&plan.type()==type,"Atomizer failed industrial fluid: "+key);
            }
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void industrialRecipesKeepOutputsAndHonorFilter(GameTestHelper h){
        if(!ModList.get().isLoaded("create_more_catalysts")){h.succeed();return;}
        var pos=new net.minecraft.core.BlockPos(2,2,2);
        h.setBlock(pos,dev.ccd.CopperDynamics.FLUID_ATOMIZER.get().defaultBlockState());
        var owner=(dev.ccd.content.FluidAtomizerBlockEntity)h.getBlockEntity(pos);
        var filter=owner.getBehaviour(com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour.TYPE);
        for(var id:java.util.List.of("preservation","salout","etch","sulfon","dehyd","oxidize","reduce","ferment")){
            var type=FanCompat.find("create_more_catalysts",id);if(type==null)continue;
            var recipe=h.getLevel().getRecipeManager().getRecipes().stream().filter(holder->
                ResourceLocation.fromNamespaceAndPath("create_more_catalysts",id).equals(BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType()))
                && !holder.value().getIngredients().isEmpty()&&holder.value().getIngredients().getFirst().getItems().length>0).findFirst();
            if(recipe.isEmpty())continue; // Upstream may disable/generated recipes through config.
            var input=recipe.get().value().getIngredients().getFirst().getItems()[0].copyWithCount(1);
            if(!type.canProcess(input,h.getLevel()))continue;
            filter.setFilter(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BARRIER));
            dev.ccd.atomizing.AtomizerProcessing.withSource(owner,()->{
                h.assertTrue(type.process(input.copy(),h.getLevel())==null,"Industrial recipe bypassed output filter: "+id);
            });
            filter.setFilter(net.minecraft.world.item.ItemStack.EMPTY);
            dev.ccd.atomizing.AtomizerProcessing.withSource(owner,()->{
                h.assertTrue(type.process(input.copy(),h.getLevel())!=null,"Industrial processing failed: "+id);
            });
        }
        h.succeed();
    }
}
