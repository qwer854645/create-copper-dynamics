package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.model.BakedQuadHelper;
import dev.ccd.CopperDynamics;
import dev.ccd.content.FluidPackageItem;
import dev.ccd.mixin.CanisterPartialModelAccessor;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Resolve the same baked fluid-window geometry for items and native cached/instanced packages. */
public final class FluidCanisterModel extends BakedModelWrapper<BakedModel> {
    private static final ResourceLocation ITEM = ResourceLocation.fromNamespaceAndPath("ccd", "fluid_package");
    private record Appearance(ResourceLocation sprite, int tint, int level) {}
    private record Variant(ResourceLocation key, PartialModel partial, BakedModel model) {}
    private static final Map<Appearance, ResourceLocation> KEYS = new HashMap<>();
    private static final Map<Appearance, Variant> VARIANTS = new HashMap<>();
    private static BakedModel shell;
    private final List<BakedQuad> quads;
    private final ItemOverrides overrides;

    public FluidCanisterModel(BakedModel original) {
        super(original);
        quads = null;
        overrides = new ItemOverrides() {
            @Override public BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel level,
                LivingEntity entity, int seed) {
                return variant(stack).model();
            }
        };
    }

    private FluidCanisterModel(BakedModel original, Appearance appearance) {
        super(original);
        overrides = ItemOverrides.EMPTY;
        quads = new ArrayList<>();
        TextureAtlasSprite sprite = appearance.sprite() == null ? null
            : Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                .apply(appearance.sprite());
        float height = (2 + 7 * appearance.level() / 20f) / 16f;
        for (BakedQuad quad : original.getQuads(null, null, RandomSource.create(0))) {
            if (quad.getTintIndex() != 0 || sprite == null) {
                quads.add(quad);
                continue;
            }
            int[] vertices = quad.getVertices().clone();
            float minY = Float.POSITIVE_INFINITY;
            for (int v = 0; v < 4; v++) minY = Math.min(minY, (float) BakedQuadHelper.getXYZ(vertices, v).y);
            if (minY >= height) continue; // The recessed dark backing remains visible above the liquid.
            int tint = appearance.tint();
            int abgr = 0xff000000 | ((tint & 255) << 16) | (tint & 0xff00) | ((tint >>> 16) & 255);
            for (int v = 0; v < 4; v++) {
                var position = BakedQuadHelper.getXYZ(vertices, v);
                position = new net.minecraft.world.phys.Vec3(position.x, Math.min(position.y, height), position.z);
                BakedQuadHelper.setXYZ(vertices, v, position);
                // Continuous UVs across the pixel silhouette; actual still sprite, including animation.
                BakedQuadHelper.setU(vertices, v, sprite.getU((float) position.x));
                BakedQuadHelper.setV(vertices, v, sprite.getV(1 - (float) position.y));
                vertices[v * BakedQuadHelper.VERTEX_STRIDE + BakedQuadHelper.COLOR_OFFSET] = abgr;
            }
            quads.add(new BakedQuad(vertices, -1, quad.getDirection(), sprite, quad.isShade()));
        }
    }

    /** Install the item wrapper during baking; atlas-dependent variants wait until upload completes. */
    public static BakedModel install(BakedModel original) {
        shell = original;
        VARIANTS.clear();
        return new FluidCanisterModel(original);
    }

    public static void rebuildTransportVariants() {
        if (shell == null) return;
        for (var entry : KEYS.entrySet()) create(entry.getKey(), entry.getValue());
    }

    public static ResourceLocation transportKey(ItemStack stack, net.minecraft.world.item.Item nativeItem) {
        if (stack.is(CopperDynamics.COMPOSITE_PACKAGE.get())) return BrassBoxModel.transportKey(stack);
        return stack.is(CopperDynamics.FLUID_PACKAGE.get()) && shell != null
            ? variant(stack).key() : BuiltInRegistries.ITEM.getKey(nativeItem);
    }

    private static Variant variant(ItemStack stack) {
        var fluid = FluidPackageItem.contents(stack);
        Appearance appearance;
        if (fluid.isEmpty()) appearance = new Appearance(null, -1, 0);
        else {
            var extension = IClientFluidTypeExtensions.of(fluid.getFluid());
            int level = Math.min(20, Math.max(1, (int) Math.ceil(fluid.getAmount() * 20d / FluidPackageItem.CAPACITY)));
            appearance = new Appearance(extension.getStillTexture(fluid), extension.getTintColor(fluid), level);
        }
        var existing = VARIANTS.get(appearance);
        if (existing != null) return existing;
        var key = KEYS.computeIfAbsent(appearance, ignored -> ResourceLocation.fromNamespaceAndPath("ccd", "canister_view/" + KEYS.size()));
        return create(appearance, key);
    }

    private static Variant create(Appearance appearance, ResourceLocation key) {
        var model = new FluidCanisterModel(shell, appearance);
        // Standalone partials deliberately stay out of Flywheel's resource-file registration map.
        var partial = CanisterPartialModelAccessor.ccd$create(key);
        ((CanisterPartialModelAccessor) (Object) partial).ccd$setModel(model);
        AllPartialModels.PACKAGES.put(key, partial);
        AllPartialModels.PACKAGE_RIGGING.put(key, AllPartialModels.PACKAGE_RIGGING.get(ITEM));
        var variant = new Variant(key, partial, model);
        VARIANTS.put(appearance, variant);
        return variant;
    }

    @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        return quads == null ? originalModel.getQuads(state, side, random) : side == null ? quads : List.of();
    }
    @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random,
        ModelData data, RenderType type) { return getQuads(state, side, random); }
    @Override public ItemOverrides getOverrides() { return overrides; }
    @Override public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
        originalModel.applyTransform(context, pose, leftHand);
        return this;
    }
    @Override public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) { return List.of(this); }
}
