package dev.ccd.atomizing;

import java.util.List;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ccd.CopperDynamics;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

/** Reloadable medium definition; ordinary recipes, datapacks and KubeJS share exactly this representation. */
public record AtomizingRecipe(FluidIngredient fluid, String processingType, Consumption consumption, int priority,
    int minimumRpm, List<ItemRule> items, List<MobEffectInstance> effects, float damage,
    List<ResourceLocation> entityTypes, String entityScript, int entityInterval, boolean enabled) implements Recipe<RecipeInput> {
    public record Consumption(int amount, int interval) {
        public static final Codec<Consumption> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 4000).fieldOf("amount").forGetter(Consumption::amount),
            Codec.intRange(1, 1200).fieldOf("interval").forGetter(Consumption::interval)).apply(i, Consumption::new));
    }
    public record Output(ItemStack stack, double chance) {
        public Output { stack = stack.copy(); }
        @Override public ItemStack stack() { return stack.copy(); }
        public static final Codec<Output> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.CODEC.fieldOf("stack").forGetter(Output::stack),
            Codec.doubleRange(0, 1).optionalFieldOf("chance", 1d).forGetter(Output::chance)).apply(i, Output::new));
    }
    public record ItemRule(Ingredient input, List<Output> results, int processingTime, String script) {
        public static final Codec<ItemRule> CODEC = RecordCodecBuilder.<ItemRule>create(i -> i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(ItemRule::input),
            Output.CODEC.listOf().optionalFieldOf("results", List.of()).forGetter(ItemRule::results),
            Codec.intRange(1, 72000).optionalFieldOf("processing_time", 100).forGetter(ItemRule::processingTime),
            Codec.STRING.optionalFieldOf("script", "").forGetter(ItemRule::script)).apply(i, ItemRule::new))
            .validate(rule -> rule.results().isEmpty() && rule.script().isEmpty()
                ? DataResult.error(() -> "An atomizing item rule needs results or a script handler") : DataResult.success(rule));
    }
    public static final MapCodec<AtomizingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        FluidIngredient.CODEC_NON_EMPTY.fieldOf("fluid").forGetter(AtomizingRecipe::fluid),
        Codec.STRING.optionalFieldOf("processing_type", "").forGetter(AtomizingRecipe::processingType),
        Consumption.CODEC.optionalFieldOf("consumption", new Consumption(10, 20)).forGetter(AtomizingRecipe::consumption),
        Codec.INT.optionalFieldOf("priority", 0).forGetter(AtomizingRecipe::priority),
        Codec.intRange(0, 256).optionalFieldOf("minimum_rpm", 0).forGetter(AtomizingRecipe::minimumRpm),
        ItemRule.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(AtomizingRecipe::items),
        MobEffectInstance.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(AtomizingRecipe::effects),
        Codec.floatRange(0, 1000000).optionalFieldOf("damage", 0f).forGetter(AtomizingRecipe::damage),
        ResourceLocation.CODEC.listOf().optionalFieldOf("entity_types", List.of()).forGetter(AtomizingRecipe::entityTypes),
        Codec.STRING.optionalFieldOf("entity_script", "").forGetter(AtomizingRecipe::entityScript),
        Codec.intRange(1, 1200).optionalFieldOf("entity_interval", 20).forGetter(AtomizingRecipe::entityInterval),
        Codec.BOOL.optionalFieldOf("enabled", true).forGetter(AtomizingRecipe::enabled)).apply(i, AtomizingRecipe::new));
    @Override public boolean matches(RecipeInput input, Level level) { return false; }
    @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<?> getSerializer() { return CopperDynamics.ATOMIZING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return CopperDynamics.ATOMIZING_TYPE.get(); }
    public static final class Serializer implements RecipeSerializer<AtomizingRecipe> {
        @Override public MapCodec<AtomizingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, AtomizingRecipe> streamCodec() {
            return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
        }
    }
}
