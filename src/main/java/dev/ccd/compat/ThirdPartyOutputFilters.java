package dev.ccd.compat;

import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import dev.ccd.content.FluidAtomizerBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Released, inspected processing paths. Never executes process() to discover results. */
public final class ThirdPartyOutputFilters {
    private ThirdPartyOutputFilters(){}
    private static boolean coloringUnavailable;
    private static final java.util.Set<String> STANDARD=java.util.Set.of(
        "dev.lopyluna.dndesires.content.fan_types.DragonBreathingType",
        "dev.lopyluna.dndesires.content.fan_types.FreezingType",
        "dev.lopyluna.dndesires.content.fan_types.SandingType",
        "dev.lopyluna.dndesires.content.fan_types.SeethingType",
        "com.adonis.createshimmer.common.kinetics.fan.glooming.GloomingFanProcessingType",
        "com.adonis.createshimmer.common.kinetics.fan.transmutation.TransmutationFanProcessingType");
    public static boolean allows(FanProcessingType type,ItemStack input,Level level,FluidAtomizerBlockEntity owner){
        String name=type.getClass().getName();
        if(STANDARD.contains(name))return true; // No alternate processing branch after RecipeApplier rejection.
        if(name.startsWith("net.dakotapride.garnished.recipe.GarnishedFanProcessing$")
            &&(name.endsWith("DyeBlowingFanProcessingType")||name.endsWith("$FreezingType")))return true;
        String prefix="plus.dragons.createdragonsplus.common.kinetics.fan.";
        if(name.equals(prefix+"coloring.ColoringFanProcessingType"))return coloring(type,input,level,owner);
        String recipe=switch(name){
            case "plus.dragons.createdragonsplus.common.kinetics.fan.ending.EndingFanProcessingType" -> "ending";
            case "plus.dragons.createdragonsplus.common.kinetics.fan.freezing.FreezingFanProcessingType" -> "freezing";
            case "plus.dragons.createdragonsplus.common.kinetics.fan.sanding.SandingFanProcessingType" -> "sanding";
            default -> null;
        };
        if(recipe==null)return false;
        var primary=find(level,"create_dragons_plus",recipe,input);
        if(primary!=null)return owner.acceptsRecipe(primary);
        if(recipe.equals("sanding")){
            var paper=com.simibubi.create.AllRecipeTypes.SANDPAPER_POLISHING.find(new SingleRecipeInput(input),level);
            if(paper.isPresent())return owner.acceptsRecipe(paper.get().value());
        }
        // Only inspect registered recipe data from known integration partners, not compat callbacks.
        var fallback=find(level,"dndesires",recipe.equals("ending")?"dragon_breathing":recipe,input);
        if(fallback!=null)return owner.acceptsRecipe(fallback);
        if(recipe.equals("freezing")){
            fallback=find(level,"garnished","freezing",input);
            if(fallback!=null)return owner.acceptsRecipe(fallback);
        }
        return false;
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static Recipe<?> find(Level level,String namespace,String path,ItemStack input){
        var key=ResourceLocation.fromNamespaceAndPath(namespace,path);
        if(!BuiltInRegistries.RECIPE_TYPE.containsKey(key))return null;
        RecipeType type=BuiltInRegistries.RECIPE_TYPE.get(key);
        return (Recipe<?>)level.getRecipeManager().getRecipeFor(type,new SingleRecipeInput(input.copy()),level)
            .map(holder->((RecipeHolder<?>)holder).value()).orElse(null);
    }
    private static final class ColoringPreview {
        static final java.lang.reflect.Method PREVIEW,OUTPUTS;
        static {
            try{
                var type=Class.forName("plus.dragons.createdragonsplus.common.kinetics.fan.coloring.ColoringFanProcessingType");
                PREVIEW=type.getMethod("processForDyeFluidMixing",ItemStack.class,Level.class);
                OUTPUTS=Class.forName(type.getName()+"$DyeFluidMixingResult").getMethod("outputs");
            }catch(ReflectiveOperationException e){throw new IllegalStateException("Unsupported Dragons Plus coloring preview API",e);}
        }
    }
    private static boolean coloring(FanProcessingType type,ItemStack input,Level level,FluidAtomizerBlockEntity owner){
        if(coloringUnavailable)return false;
        try{
            var preview=(java.util.Optional<?>)ColoringPreview.PREVIEW.invoke(type,input.copyWithCount(1),level);
            if(preview.isEmpty())return false;
            var outputs=(java.util.List<?>)ColoringPreview.OUTPUTS.invoke(preview.get());
            for(var value:outputs){
                var output=(com.simibubi.create.content.processing.recipe.ProcessingOutput)value;
                if(output.getChance()>0&&owner.acceptsOutput(output.getStack()))return true;
            }
            return false;
        }catch(ReflectiveOperationException | LinkageError | RuntimeException e){
            coloringUnavailable=true;
            org.slf4j.LoggerFactory.getLogger("CCD-DragonsPlus").error("Pausing CCD filtered coloring: unsupported preview API",e);
            return false;
        }
    }
}
