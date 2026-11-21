package dev.ccd.atomizing;

import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.List;

/** Live crafting lookup: exactly one workpiece and one slimeball, with no lost remainders. */
public final class BulkGluingType implements FanProcessingType {
    private static ItemStack result(ItemStack input,Level level){
        if(input.isEmpty()||input.is(Items.SLIME_BALL))return ItemStack.EMPTY;
        for(var holder:level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)){
            var recipe=holder.value();
            var ingredients=recipe.getIngredients().stream().filter(i->!i.isEmpty()).toList();
            if(ingredients.size()!=2)continue;
            if(!(ingredients.get(0).test(input)&&ingredients.get(1).test(new ItemStack(Items.SLIME_BALL)))
                &&!(ingredients.get(1).test(input)&&ingredients.get(0).test(new ItemStack(Items.SLIME_BALL))))continue;
            // Match the real crafting grid, so shaped recipes and custom outputs remain valid.
            for(int a=0;a<9;a++)for(int b=0;b<9;b++)if(a!=b){
                var grid=new java.util.ArrayList<ItemStack>(java.util.Collections.nCopies(9,ItemStack.EMPTY));
                grid.set(a,input.copyWithCount(1));grid.set(b,new ItemStack(Items.SLIME_BALL));
                var crafting=CraftingInput.of(3,3,grid);
                if(!recipe.matches(crafting,level))continue;
                if(recipe.getRemainingItems(crafting).stream().anyMatch(s->!s.isEmpty()))continue;
                return recipe.assemble(crafting,level.registryAccess());
            }
        }
        return ItemStack.EMPTY;
    }
    @Override public boolean isValidAt(Level level,net.minecraft.core.BlockPos pos){return level.getFluidState(pos).getFluidType()==dev.ccd.CCDFluids.SLIME_TYPE.get();}
    @Override public int getPriority(){return 450;}
    @Override public boolean canProcess(ItemStack stack,Level level){return !result(stack,level).isEmpty();}
    @Override public List<ItemStack> process(ItemStack stack,Level level){
        var output=result(stack,level);if(output.isEmpty())return null;
        var results=new java.util.ArrayList<ItemStack>();
        long amount=(long)output.getCount()*stack.getCount();
        while(amount>0){int count=(int)Math.min(output.getMaxStackSize(),amount);results.add(output.copyWithCount(count));amount-=count;}
        return results;
    }
    @Override public void spawnProcessingParticles(Level level,net.minecraft.world.phys.Vec3 pos){level.addParticle(net.minecraft.core.particles.ParticleTypes.ITEM_SLIME,pos.x,pos.y,pos.z,0,.02,0);}
    @Override public void morphAirFlow(AirFlowParticleAccess particle,net.minecraft.util.RandomSource random){particle.setColor(0x82c66a);particle.setAlpha(.5f);}
    @Override public void affectEntity(net.minecraft.world.entity.Entity entity,Level level){}
}
