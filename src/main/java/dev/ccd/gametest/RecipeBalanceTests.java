package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class RecipeBalanceTests {
    @GameTest(template="empty") public static void survivalRecipesLoadWithoutCreativeOutputs(GameTestHelper h){
        var manager=h.getLevel().getRecipeManager();
        for(var id:java.util.List.of("copper_bucket","brass_bucket","booster_pump","bidirectional_fluid_pump","redstone_fluid_pump","buffer_pump","omni_pump","fluid_atomizer","fluid_bridge","copper_water_supply","smart_fluid_tank"))
            h.assertTrue(manager.byKey(ResourceLocation.fromNamespaceAndPath("ccd",id)).isPresent(),"Missing survival recipe: "+id);
        var tank=manager.byKey(ResourceLocation.fromNamespaceAndPath("ccd","smart_fluid_tank")).orElseThrow().value();
        h.assertTrue(tank.getResultItem(h.getLevel().registryAccess()).getCount()==1,"Smart tank recipe duplicates capacity");
        var turn=manager.byKey(ResourceLocation.fromNamespaceAndPath("ccd","turning_booster_pump")).orElseThrow().value();
        h.assertTrue(turn.getType()==RecipeType.STONECUTTING&&turn.getResultItem(h.getLevel().registryAccess()).getCount()==1,"Shape conversions must be explicit 1:1 choices");
        for(var holder:manager.getRecipes()) {
            var result=holder.value().getResultItem(h.getLevel().registryAccess());
            h.assertTrue(!result.is(CopperDynamics.CREATIVE_DELIVERY_TANK_ITEM.get()),"Creative tank has a survival recipe: "+holder.id());
        }
        var lava=(com.simibubi.create.content.processing.recipe.ProcessingRecipe<?,?>)manager.byKey(ResourceLocation.fromNamespaceAndPath("ccd","mixing/seething_reagent")).orElseThrow().value();
        h.assertTrue(lava.getIngredients().stream().anyMatch(i->i.test(com.simibubi.create.AllItems.BLAZE_CAKE.asStack())),"Super lava fuel loop is not gated by a blaze cake");
        h.assertTrue(lava.getFluidResults().getFirst().getAmount()==500,"Super lava batch must be limited");
        h.succeed();
    }
}
