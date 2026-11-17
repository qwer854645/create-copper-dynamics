package dev.ccd.client;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelState;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelType;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelModel;
import com.simibubi.create.foundation.model.BakedQuadHelper;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Native panel assembly and transforms, with CCD's frame-only recoloring of the two sprite atlases. */
public final class FluidGaugeModel extends FactoryPanelModel {
    private static final ResourceLocation NORMAL = ResourceLocation.fromNamespaceAndPath("create", "block/factory_panel");
    private static final ResourceLocation RESTOCKER = ResourceLocation.fromNamespaceAndPath("create", "block/factory_panel_packager_mode");
    // Strong references keep both dependencies registered in Flywheel's weak partial-model map.
    private static final List<PartialModel> DEPENDENCIES = List.of(
        PartialModel.of(ResourceLocation.fromNamespaceAndPath("ccd", "block/fluid_gauge/panel")),
        PartialModel.of(ResourceLocation.fromNamespaceAndPath("ccd", "block/fluid_gauge/panel_restocker")));
    private final TextureAtlasSprite normal, restocker;

    public static void registerModels() {} // Initialize and retain dependencies before model baking.

    public FluidGaugeModel(BakedModel original, TextureAtlasSprite normal, TextureAtlasSprite restocker) {
        super(original);
        this.normal = normal;
        this.restocker = restocker;
    }

    @Override public void addPanel(List<BakedQuad> quads, BlockState state, PanelSlot slot, PanelType type,
        PanelState panelState, RandomSource random, ModelData data, RenderType renderType, boolean ponder) {
        int start = quads.size();
        super.addPanel(quads, state, slot, type, panelState, random, data, renderType, ponder);
        for (int index = start; index < quads.size(); index++) {
            var quad = quads.get(index);
            var source = quad.getSprite();
            var id = source.contents().name();
            var target = id.equals(NORMAL) ? normal : id.equals(RESTOCKER) ? restocker : null;
            if (target == null) continue;
            int[] vertices = quad.getVertices().clone();
            for (int vertex = 0; vertex < 4; vertex++) {
                float u = (BakedQuadHelper.getU(vertices, vertex) - source.getU0()) / (source.getU1() - source.getU0());
                float v = (BakedQuadHelper.getV(vertices, vertex) - source.getV0()) / (source.getV1() - source.getV0());
                BakedQuadHelper.setU(vertices, vertex, target.getU(u));
                BakedQuadHelper.setV(vertices, vertex, target.getV(v));
            }
            quads.set(index, new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), target, quad.isShade()));
        }
    }
}
