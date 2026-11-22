package dev.ccd.atomizing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.ccd.CopperDynamics;
import dev.ccd.api.AtomizerScripts;
import dev.ccd.content.FluidAtomizerBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Source context around native fan processing; callbacks never receive live inventory or entity references. */
public final class AtomizerProcessing {
    private AtomizerProcessing() {}
    private static final ThreadLocal<FluidAtomizerBlockEntity> SOURCE = new ThreadLocal<>();
    private static final Map<TransportedItemStack, String> KEYS = new WeakHashMap<>();
    public static FluidAtomizerBlockEntity source() { return SOURCE.get(); }
    public static boolean supportsOutputFilter(FanProcessingType type,ItemStack input,net.minecraft.world.level.Level level){
        var owner=source();
        if(owner==null||!owner.hasOutputFilter())return true;
        if(type==CopperDynamics.ATOMIZING_FAN_TYPE.get()||type==CopperDynamics.BULK_GLUING.get())return true;
        // Allow only routes whose recipe execution is guarded before output rolling.
        if(type==CopperDynamics.SANDBLASTING.get()||type==CopperDynamics.FLUID_HAUNTING.get())return true;
        var key=com.simibubi.create.api.registry.CreateBuiltInRegistries.FAN_PROCESSING_TYPE.getKey(type);
        if(key==null||com.simibubi.create.api.registry.CreateBuiltInRegistries.FAN_PROCESSING_TYPE.get(key)!=type)return false;
        if(type==com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes.BLASTING){
            var recipeInput=new net.minecraft.world.item.crafting.SingleRecipeInput(input);
            var manager=level.getRecipeManager();
            return manager.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,recipeInput,level)
                .filter(com.simibubi.create.AllRecipeTypes.CAN_BE_AUTOMATED).isPresent()
                ||manager.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.BLASTING,recipeInput,level)
                .filter(com.simibubi.create.AllRecipeTypes.CAN_BE_AUTOMATED).isPresent();
        }
        if(key.getNamespace().equals("create"))return true;
        // More Catalysts common types execute through RecipeApplier (verified release 1.2.4).
        return key.getNamespace().equals("create_more_catalysts")
            ||dev.ccd.compat.ThirdPartyOutputFilters.allows(type,input,level,owner);
    }
    public static void withSource(FluidAtomizerBlockEntity owner, Runnable work) {
        var previous = SOURCE.get();
        SOURCE.set(owner);
        try { work.run(); }
        finally { if (previous == null) SOURCE.remove(); else SOURCE.set(previous); }
    }
    private static boolean custom(FanProcessingType type) { return type == CopperDynamics.ATOMIZING_FAN_TYPE.get() && source() != null; }
    private static int duration(ItemStack stack) {
        var plan = source().itemRule(stack);
        int ticks = plan == null ? AllConfigs.server().kinetics.fanProcessingTime.get() : plan.processingTime();
        return (int) Math.min(Integer.MAX_VALUE - 1, (long) ticks * ((stack.getCount() - 1) / 16 + 1));
    }
    public static boolean canProcess(ItemStack stack) {
        var owner = source();
        if (owner == null || !owner.mistActive()) return false;
        if (owner.itemRule(stack) != null) return acceptsRule(owner,owner.itemRule(stack));
        var fallback = owner.nativeFallback();
        return fallback != null && fallback.canProcess(stack, owner.getLevel());
    }
    public static void prepare(TransportedItemStack item, FanProcessingType type) {
        if (source()==null) return;
        String key = source().processingKey(item.stack)+":"+com.simibubi.create.api.registry.CreateBuiltInRegistries.FAN_PROCESSING_TYPE.getKey(type);
        if (item.processedBy == type && key.equals(KEYS.get(item))) return;
        KEYS.put(item, key);
        if(!custom(type)) { item.processedBy=null; return; }
        item.processedBy = type;
        item.processingTime = canProcess(item.stack) ? duration(item.stack) : -1;
    }
    public static void prepare(ItemEntity entity, FanProcessingType type) {
        if (source()==null) return;
        CompoundTag root = entity.getPersistentData();
        String key = source().processingKey(entity.getItem())+":"+com.simibubi.create.api.registry.CreateBuiltInRegistries.FAN_PROCESSING_TYPE.getKey(type);
        CompoundTag create = root.getCompound("CreateData");
        CompoundTag progress = create.getCompound("Processing");
        if (key.equals(root.getString("CCDAtomizingRule"))) return;
        root.putString("CCDAtomizingRule", key);
        if(!custom(type)) { create.remove("Processing");root.put("CreateData",create);return; }
        progress.putString("Type", "ccd:atomizing");
        progress.putInt("Time", canProcess(entity.getItem()) ? duration(entity.getItem()) : -1);
        create.put("Processing", progress);
        root.put("CreateData", create);
    }
    public static List<ItemStack> process(ItemStack input) {
        var owner = source();
        if (owner == null || !owner.mistActive()) return null;
        var rule = owner.itemRule(input);
        if(rule!=null&&!acceptsRule(owner,rule))return null;
        if (rule == null) {
            var fallback = owner.nativeFallback();
            return fallback == null || !supportsOutputFilter(fallback,input,owner.getLevel()) ? null : fallback.process(input, owner.getLevel());
        }
        List<ItemStack> results = new ArrayList<>();
        if (!rule.script().isEmpty()) {
            var result = AtomizerScripts.processItem(rule.script(), new AtomizerScripts.ItemContext(input, owner.activeFluid(),
                owner.getSpeed(), owner.getBlockPos(), owner.getLevel().dimension().location()));
            if(result==null)return null;
            var outputs=result.outputs();
            if(owner.hasOutputFilter()&&outputs.stream().noneMatch(owner::acceptsOutput))return null;
            for (var stack : outputs) split(results, stack, stack.getCount());
        } else for (var output : rule.results()) {
            ItemStack stack = output.stack();
            long amount = 0;
            for (int i = 0; i < input.getCount(); i++) if (owner.getLevel().random.nextDouble() < output.chance()) amount += stack.getCount();
            split(results, stack, amount);
        }
        return results;
    }
    private static boolean acceptsRule(FluidAtomizerBlockEntity owner,AtomizingRecipe.ItemRule rule) {
        // Script callbacks are not safe to execute speculatively: require declared results
        // when a target filter is installed, then check their actual output before conversion.
        return !owner.hasOutputFilter()||rule.results().stream().anyMatch(output->output.chance()>0&&owner.acceptsOutput(output.stack()));
    }
    private static void split(List<ItemStack> results, ItemStack stack, long amount) {
        if (stack.isEmpty()) return;
        while (amount > 0) {
            int count = (int) Math.min(amount, stack.getMaxStackSize());
            results.add(stack.copyWithCount(count));
            amount -= count;
        }
    }
}
