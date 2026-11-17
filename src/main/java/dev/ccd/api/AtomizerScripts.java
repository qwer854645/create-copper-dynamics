package dev.ccd.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import com.mojang.logging.LogUtils;
import dev.ccd.atomizing.AtomizingRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** KubeJS-independent API. Contexts expose copies and values; CCD commits returned results. */
public final class AtomizerScripts {
    private AtomizerScripts() {}
    public record ItemContext(ItemStack input, FluidStack fluid, float speed, BlockPos position, ResourceLocation dimension) {
        public ItemContext { input = input.copy(); fluid = fluid.copy(); position = position.immutable(); }
        @Override public ItemStack input() { return input.copy(); }
        @Override public FluidStack fluid() { return fluid.copy(); }
        public ItemStack getInput() { return input(); }
        public FluidStack getFluid() { return fluid(); }
        public float getSpeed() { return speed; }
        public BlockPos getPosition() { return position; }
        public ResourceLocation getDimension() { return dimension; }
    }
    public record EntityContext(ResourceLocation entityType, UUID uuid, float health, FluidStack fluid,
        float speed, BlockPos position, ResourceLocation dimension) {
        public EntityContext { fluid = fluid.copy(); position = position.immutable(); }
        @Override public FluidStack fluid() { return fluid.copy(); }
        public ResourceLocation getEntityType() { return entityType; }
        public UUID getUuid() { return uuid; }
        public float getHealth() { return health; }
        public FluidStack getFluid() { return fluid(); }
        public float getSpeed() { return speed; }
        public BlockPos getPosition() { return position; }
        public ResourceLocation getDimension() { return dimension; }
    }
    public record ItemResult(List<ItemStack> outputs) {
        public ItemResult { outputs = outputs.stream().map(ItemStack::copy).toList(); }
        @Override public List<ItemStack> outputs() { return new ArrayList<>(outputs.stream().map(ItemStack::copy).toList()); }
    }
    public record EntityResult(List<MobEffectInstance> effects, float damage) {
        public EntityResult {
            effects = effects.stream().map(MobEffectInstance::new).toList();
            if (!Float.isFinite(damage) || damage < 0) throw new IllegalArgumentException("Invalid atomizer damage");
        }
        @Override public List<MobEffectInstance> effects() { return effects.stream().map(MobEffectInstance::new).toList(); }
    }
    private static final Map<String, Function<ItemContext, ItemResult>> ITEMS = new ConcurrentHashMap<>();
    private static final Map<String, Function<EntityContext, EntityResult>> ENTITIES = new ConcurrentHashMap<>();
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();
    public static void itemHandler(String id, Function<ItemContext, ItemResult> handler) { ITEMS.put(ResourceLocation.parse(id).toString(), handler); }
    public static void entityHandler(String id, Function<EntityContext, EntityResult> handler) { ENTITIES.put(ResourceLocation.parse(id).toString(), handler); }
    public static ItemResult output(ItemStack stack) { return new ItemResult(List.of(stack)); }
    public static ItemResult outputs(ItemStack... stacks) { return new ItemResult(List.of(stacks)); }
    public static EntityResult damage(float amount) { return new EntityResult(List.of(), amount); }
    public static EntityResult effect(String id, int ticks, int amplifier) {
        var effect = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(id)).orElseThrow();
        return new EntityResult(List.of(new MobEffectInstance(effect, ticks, amplifier)), 0);
    }
    public static void clear() { ITEMS.clear(); ENTITIES.clear(); REPORTED.clear(); AtomizingRules.invalidate(); }
    public static ItemResult processItem(String id, ItemContext context) {
        try {
            var handler = ITEMS.get(id);
            if (handler == null) throw new IllegalArgumentException("Unknown item handler " + id);
            return handler.apply(context);
        } catch (RuntimeException e) { report(id, context.position(), e); return null; }
    }
    public static EntityResult processEntity(String id, EntityContext context) {
        try {
            var handler = ENTITIES.get(id);
            if (handler == null) throw new IllegalArgumentException("Unknown entity handler " + id);
            return handler.apply(context);
        } catch (RuntimeException e) { report(id, context.position(), e); return null; }
    }
    private static void report(String id, BlockPos pos, RuntimeException error) {
        if (REPORTED.add(id)) LogUtils.getLogger().error("Atomizer script {} failed at {} (further errors suppressed until reload)", id, pos, error);
    }
}
