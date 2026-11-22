package dev.ccd.gametest;

import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import dev.ccd.CopperDynamics;
import dev.ccd.atomizing.AtomizerProcessing;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class AtomizerOutputFilterTests {
    @GameTest(template="empty") public static void outputFilterGuardsNativeRecipeAndPersists(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);
        h.setBlock(pos,CopperDynamics.FLUID_ATOMIZER.get().defaultBlockState());
        var atomizer=(dev.ccd.content.FluidAtomizerBlockEntity)h.getBlockEntity(pos);
        var filter=atomizer.getBehaviour(FilteringBehaviour.TYPE);
        var input=new ItemStack(Items.IRON_ORE);
        var recipe=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SingleRecipeInput(input),h.getLevel()).orElseThrow().value();
        filter.setFilter(new ItemStack(Items.GOLD_INGOT));
        AtomizerProcessing.withSource(atomizer,()->{
            h.assertTrue(RecipeApplier.applyRecipeOn(h.getLevel(),input,recipe,false)==null,"Rejected recipe consumed input");
            h.assertTrue(input.is(Items.IRON_ORE)&&input.getCount()==1,"Input changed on rejection");
        });
        filter.setFilter(new ItemStack(Items.IRON_INGOT));
        AtomizerProcessing.withSource(atomizer,()->{
            var results=RecipeApplier.applyRecipeOn(h.getLevel(),input,recipe,false);
            h.assertTrue(results!=null&&results.stream().anyMatch(stack->stack.is(Items.IRON_INGOT)),"Matching output rejected");
        });
        var tag=atomizer.saveWithoutMetadata(h.getLevel().registryAccess());
        filter.setFilter(ItemStack.EMPTY);
        atomizer.loadWithComponents(tag,h.getLevel().registryAccess());
        h.assertTrue(atomizer.hasOutputFilter()&&atomizer.acceptsOutput(new ItemStack(Items.IRON_INGOT))&&!atomizer.acceptsOutput(new ItemStack(Items.GOLD_INGOT)),"Output filter did not persist");
        filter.setFilter(ItemStack.EMPTY);
        h.assertTrue(atomizer.acceptsRecipe(recipe),"Empty filter must allow recipes");h.succeed();
    }
}
