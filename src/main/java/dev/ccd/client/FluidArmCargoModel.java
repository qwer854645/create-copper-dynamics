package dev.ccd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.model.BakedQuadHelper;
import dev.ccd.content.FluidPackageItem;
import dev.ccd.logistics.FluidArmPoint;
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
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Only the liquid: Create's held-item transform seats it inside the fixed syringe partial. */
public final class FluidArmCargoModel extends BakedModelWrapper<BakedModel> {
    private record Appearance(ResourceLocation sprite, int tint, int level) {}
    private final List<BakedQuad> quads;
    private final ItemOverrides overrides;

    public FluidArmCargoModel(BakedModel original) {
        super(original);
        quads = List.of();
        // Each baked wrapper owns its atlas-dependent cache; resource reload discards it.
        Map<Appearance, BakedModel> variants = new HashMap<>();
        overrides = new ItemOverrides() {
            @Override public BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel level,
                LivingEntity entity, int seed) {
                var fluid = FluidPackageItem.contents(stack);
                if (fluid.isEmpty()) return FluidArmCargoModel.this;
                var extension = IClientFluidTypeExtensions.of(fluid.getFluid());
                int fill = Math.min(20, Math.max(1, (int) Math.ceil(fluid.getAmount() * 20d / FluidArmPoint.BATCH)));
                var appearance = new Appearance(extension.getStillTexture(fluid), extension.getTintColor(fluid), fill);
                return variants.computeIfAbsent(appearance, key -> new FluidArmCargoModel(original, key));
            }
        };
    }

    private FluidArmCargoModel(BakedModel original, Appearance appearance) {
        super(original);
        overrides = ItemOverrides.EMPTY;
        quads = new ArrayList<>();
        if (appearance.sprite() == null) return;
        var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(appearance.sprite());
        // Inverse of native FIXED item placement: local syringe Z [-10.8,-5.2].
        float height = (6.72f + 8.96f * appearance.level() / 20f) / 16f;
        int tint = appearance.tint();
        int abgr = 0xff000000 | ((tint & 255) << 16) | (tint & 0xff00) | ((tint >>> 16) & 255);
        for (var quad : original.getQuads(null, null, RandomSource.create(0))) {
            int[] vertices = quad.getVertices().clone();
            for (int v = 0; v < 4; v++) {
                var p = BakedQuadHelper.getXYZ(vertices, v);
                p = new Vec3(p.x, Math.min(p.y, height), p.z);
                BakedQuadHelper.setXYZ(vertices, v, p);
                // Preserve a closed liquid surface when the barrel is partly full.
                float u = (float) (quad.getDirection().getAxis() == Direction.Axis.X ? p.z : p.x);
                float uvV = (float) (quad.getDirection().getAxis() == Direction.Axis.Y ? p.z : 1 - p.y);
                BakedQuadHelper.setU(vertices, v, sprite.getU(u));
                BakedQuadHelper.setV(vertices, v, sprite.getV(uvV));
                vertices[v * BakedQuadHelper.VERTEX_STRIDE + BakedQuadHelper.COLOR_OFFSET] = abgr;
            }
            quads.add(new BakedQuad(vertices, -1, quad.getDirection(), sprite, quad.isShade()));
        }
    }

    @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        return side == null ? quads : List.of();
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
