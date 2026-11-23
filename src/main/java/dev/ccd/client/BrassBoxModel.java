package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.model.BakedQuadHelper;
import dev.ccd.content.CompositePackageItem;
import dev.ccd.mixin.CanisterPartialModelAccessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

/** One stable random sample per window, shared by ordinary and instanced package rendering. */
public final class BrassBoxModel extends BakedModelWrapper<BakedModel> {
    private static final ResourceLocation ITEM = ResourceLocation.fromNamespaceAndPath("ccd", "composite_package");
    private record FluidView(ResourceLocation sprite, int tint) {}
    private record ItemView(ItemStack stack) {
        @Override public boolean equals(Object other) {
            return other instanceof ItemView view && ItemStack.isSameItemSameComponents(stack, view.stack);
        }
        @Override public int hashCode() { return ItemStack.hashItemAndComponents(stack); }
    }
    private record Appearance(ResourceLocation item,List<FluidView> fluids, List<ItemView> items) {}
    private record Variant(ResourceLocation key, BakedModel model) {}
    private static final Map<Appearance, ResourceLocation> KEYS = new HashMap<>();
    private static final Map<Appearance, Variant> VARIANTS = new HashMap<>();
    private static final Map<ResourceLocation,BakedModel> SHELLS=new HashMap<>();
    private final List<BakedQuad> quads;
    private final ItemOverrides overrides;

    private BrassBoxModel(BakedModel original) {
        super(original);
        quads = null;
        overrides = new ItemOverrides() {
            @Override public BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel level,
                LivingEntity entity, int seed) { return variant(stack).model(); }
        };
    }

    private BrassBoxModel(BakedModel original, Appearance appearance) {
        super(original);
        overrides = ItemOverrides.EMPTY;
        quads = new ArrayList<>();
        var minecraft = Minecraft.getInstance();
        double minX=1,maxX=0,minZ=1,maxZ=0,minY=1,maxY=0;
        for(var quad:original.getQuads(null,null,RandomSource.create(0))) for(int v=0;v<4;v++) {
            var p=BakedQuadHelper.getXYZ(quad.getVertices(),v);
            minX=Math.min(minX,p.x);maxX=Math.max(maxX,p.x);
            minZ=Math.min(minZ,p.z);maxZ=Math.max(maxZ,p.z);
            minY=Math.min(minY,p.y);maxY=Math.max(maxY,p.y);
        }
        for (var quad : original.getQuads(null, null, RandomSource.create(0))) {
            int index = quad.getTintIndex();
            if (index < 0) { quads.add(quad); continue; }
            if(appearance.fluids().isEmpty()) continue;
            var sprite = minecraft.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                .apply(ResourceLocation.fromNamespaceAndPath("ccd", "item/fluid_canister/fluid"));
            int tint = -1;
            if (!appearance.fluids().isEmpty()) {
                var fluid = appearance.fluids().get(index % appearance.fluids().size());
                sprite = minecraft.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(fluid.sprite());
                tint = fluid.tint();
            }
            int[] vertices = quad.getVertices().clone();
            for (int v = 0; v < 4; v++) {
                var p = BakedQuadHelper.getXYZ(vertices, v);
                BakedQuadHelper.setU(vertices, v, sprite.getU((float) p.x));
                BakedQuadHelper.setV(vertices, v, sprite.getV(1 - (float) p.y));
                color(vertices, v, tint);
            }
            quads.add(new BakedQuad(vertices, -1, quad.getDirection(), sprite, quad.isShade()));
        }
        for (int side = 0; side < 2 && !appearance.items().isEmpty(); side++) {
            var stack = appearance.items().get(side % appearance.items().size()).stack();
            var model = minecraft.getItemRenderer().getModel(stack, minecraft.level, null, 0);
            var parts = new ArrayList<BakedQuad>();
            for (var pass : model.getRenderPasses(stack, false)) {
                parts.addAll(pass.getQuads(null, null, RandomSource.create(0)));
                for (var direction : Direction.values())
                    parts.addAll(pass.getQuads(null, direction, RandomSource.create(0)));
            }
            for (var quad : parts) {
                int[] vertices = quad.getVertices().clone();
                for (int v = 0; v < 4; v++) {
                    var p = BakedQuadHelper.getXYZ(vertices, v);
                    // Place a miniature of the actual baked item behind the side glass.
                    double x = side == 0 ? minX+.006+p.z*.0005 : maxX-.006-p.z*.0005;
                    double z = minZ+(maxZ-minZ)*(.25+(side==0?1-p.x:p.x)*.5);
                    BakedQuadHelper.setXYZ(vertices, v, new Vec3(x,minY+(maxY-minY)*(.15+p.y*.5),z));
                    if (quad.isTinted()) color(vertices, v, minecraft.getItemColors().getColor(stack, quad.getTintIndex()));
                }
                var d = quad.getDirection();
                var direction = switch (d) {
                    case UP, DOWN -> d;
                    case NORTH -> side == 0 ? Direction.WEST : Direction.EAST;
                    case SOUTH -> side == 0 ? Direction.EAST : Direction.WEST;
                    case EAST -> side == 0 ? Direction.NORTH : Direction.SOUTH;
                    case WEST -> side == 0 ? Direction.SOUTH : Direction.NORTH;
                };
                quads.add(new BakedQuad(vertices, -1, direction, quad.getSprite(), false));
            }
        }
    }

    private static void color(int[] vertices, int vertex, int tint) {
        vertices[vertex * BakedQuadHelper.VERTEX_STRIDE + BakedQuadHelper.COLOR_OFFSET] =
            0xff000000 | ((tint & 255) << 16) | (tint & 0xff00) | ((tint >>> 16) & 255);
    }

    public static BakedModel install(ResourceLocation item,BakedModel original) {
        SHELLS.put(item,original);
        VARIANTS.clear();
        return new BrassBoxModel(original);
    }

    public static void rebuildTransportVariants() {
        if (!SHELLS.isEmpty()) for (var entry : KEYS.entrySet()) create(entry.getKey(), entry.getValue());
    }
    public static void clearWorldVariants() {
        for (var key : KEYS.values()) {
            AllPartialModels.PACKAGES.remove(key);
            AllPartialModels.PACKAGE_RIGGING.remove(key);
        }
        KEYS.clear(); VARIANTS.clear();
    }

    public static ResourceLocation transportKey(ItemStack stack) {
        var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return !SHELLS.containsKey(item) ? item : variant(stack).key();
    }

    private static Variant variant(ItemStack stack) {
        var fluids = new ArrayList<FluidView>();
        for (var fluid : CompositePackageItem.fluids(stack)) {
            if (fluid.isEmpty()) continue;
            var extension = IClientFluidTypeExtensions.of(fluid.getFluid());
            fluids.add(new FluidView(extension.getStillTexture(fluid), extension.getTintColor(fluid)));
        }
        var items = new ArrayList<ItemView>();
        var contents = PackageItem.getContents(stack);
        for (int slot = 0; slot < contents.getSlots(); slot++) {
            var item = contents.getStackInSlot(slot);
            if (!item.isEmpty()) {
                var view = new ItemView(item.copyWithCount(1));
                if (!items.contains(view)) items.add(view);
            }
        }
        // Content-derived randomness is stable across frames, render paths and reloads.
        var random = new java.util.Random(31L * fluids.hashCode() + items.hashCode());
        java.util.Collections.shuffle(fluids, random);
        java.util.Collections.shuffle(items, random);
        var appearance = new Appearance(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()),List.copyOf(fluids.subList(0, Math.min(2, fluids.size()))),
            List.copyOf(items.subList(0, Math.min(2, items.size()))));
        var existing = VARIANTS.get(appearance);
        if (existing != null) return existing;
        var key = KEYS.computeIfAbsent(appearance, ignored -> ResourceLocation.fromNamespaceAndPath("ccd", "brass_box_view/" + KEYS.size()));
        return create(appearance, key);
    }

    private static Variant create(Appearance appearance, ResourceLocation key) {
        var model = new BrassBoxModel(SHELLS.get(appearance.item()), appearance);
        var partial = CanisterPartialModelAccessor.ccd$create(key);
        ((CanisterPartialModelAccessor) (Object) partial).ccd$setModel(model);
        AllPartialModels.PACKAGES.put(key, partial);
        AllPartialModels.PACKAGE_RIGGING.put(key, AllPartialModels.PACKAGE_RIGGING.get(appearance.item()));
        var variant = new Variant(key, model);
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
