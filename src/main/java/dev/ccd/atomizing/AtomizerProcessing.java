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
        if (owner.itemRule(stack) != null) return true;
        var fallback = owner.nativeFallback();
        return fallback != null && fallback.canProcess(stack, owner.getLevel());
    }
    public static void prepare(TransportedItemStack item, FanProcessingType type) {
        if (!custom(type)) return;
        String key = source().processingKey(item.stack);
        if (item.processedBy == type && key.equals(KEYS.get(item))) return;
        KEYS.put(item, key);
        item.processedBy = type;
        item.processingTime = canProcess(item.stack) ? duration(item.stack) : -1;
    }
    public static void prepare(ItemEntity entity, FanProcessingType type) {
        if (!custom(type)) return;
        CompoundTag root = entity.getPersistentData();
        String key = source().processingKey(entity.getItem());
        CompoundTag create = root.getCompound("CreateData");
        CompoundTag progress = create.getCompound("Processing");
        if (key.equals(root.getString("CCDAtomizingRule")) && "ccd:atomizing".equals(progress.getString("Type"))) return;
        root.putString("CCDAtomizingRule", key);
        progress.putString("Type", "ccd:atomizing");
        progress.putInt("Time", canProcess(entity.getItem()) ? duration(entity.getItem()) : -1);
        create.put("Processing", progress);
        root.put("CreateData", create);
    }
    public static List<ItemStack> process(ItemStack input) {
        var owner = source();
        if (owner == null || !owner.mistActive()) return null;
        var rule = owner.itemRule(input);
        if (rule == null) {
            var fallback = owner.nativeFallback();
            return fallback == null ? null : fallback.process(input, owner.getLevel());
        }
        List<ItemStack> results = new ArrayList<>();
        if (!rule.script().isEmpty()) {
            var result = AtomizerScripts.processItem(rule.script(), new AtomizerScripts.ItemContext(input, owner.activeFluid(),
                owner.getSpeed(), owner.getBlockPos(), owner.getLevel().dimension().location()));
            if (result == null) return null;
            for (var stack : result.outputs()) split(results, stack, stack.getCount());
        } else for (var output : rule.results()) {
            ItemStack stack = output.stack();
            long amount = 0;
            for (int i = 0; i < input.getCount(); i++) if (owner.getLevel().random.nextDouble() < output.chance()) amount += stack.getCount();
            split(results, stack, amount);
        }
        return results;
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
