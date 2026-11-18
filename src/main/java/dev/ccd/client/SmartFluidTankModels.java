package dev.ccd.client;

import com.simibubi.create.content.fluids.tank.FluidTankCTBehaviour;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

/** Native tank connectivity and connected textures, with CCD's brass outer and inner materials. */
public final class SmartFluidTankModels {
    private static final FluidTankCTBehaviour BEHAVIOUR = new FluidTankCTBehaviour(
        shift("fluid_tank"), shift("fluid_tank_top"), shift("fluid_tank_inner"));

    private static CTSpriteShiftEntry shift(String name) {
        var base = ResourceLocation.fromNamespaceAndPath("ccd", "block/smart_fluid_tank/" + name);
        return CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, base, base.withSuffix("_connected"));
    }

    public static void registerModels() {} // Register sprite shifts before atlas loading.
    public static BakedModel wrap(BakedModel original) { return new CTModel(original, BEHAVIOUR); }
    public static BakedModel wrapHorizontal(BakedModel original) {
        return new CTModel(original,new FluidTankCTBehaviour(shift("fluid_tank"),shift("fluid_tank_top"),shift("fluid_tank_inner")) {
            @Override public CTSpriteShiftEntry getShift(net.minecraft.world.level.block.state.BlockState state,
                net.minecraft.core.Direction face,net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
                // Create's Base.getDataType queries getShift with a null sprite
                // while gathering CT model data, before any face texture is selected.
                if(sprite != null && sprite.contents().name().getPath().endsWith("fluid_tank_inner")) return shift("fluid_tank_inner");
                return face.getAxis()==state.getValue(dev.ccd.content.HorizontalSmartTankBlock.AXIS)?shift("fluid_tank_top"):shift("fluid_tank");
            }
            @Override public boolean connectsTo(net.minecraft.world.level.block.state.BlockState state,
                net.minecraft.world.level.block.state.BlockState other,net.minecraft.world.level.BlockAndTintGetter world,
                net.minecraft.core.BlockPos pos,net.minecraft.core.BlockPos otherPos,net.minecraft.core.Direction face) {
                return state.is(other.getBlock()) && world.getBlockEntity(pos) instanceof dev.ccd.content.HorizontalSmartTankBlockEntity a
                    && world.getBlockEntity(otherPos) instanceof dev.ccd.content.HorizontalSmartTankBlockEntity b
                    && a.getMainConnectionAxis()==b.getMainConnectionAxis() && a.getController().equals(b.getController());
            }
            @Override protected net.minecraft.core.Direction getUpDirection(net.minecraft.world.level.BlockAndTintGetter world,
                net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state,net.minecraft.core.Direction face) {
                var axis=state.getValue(dev.ccd.content.HorizontalSmartTankBlock.AXIS);
                return face.getAxis()==axis?net.minecraft.core.Direction.UP:net.minecraft.core.Direction.get(net.minecraft.core.Direction.AxisDirection.POSITIVE,axis);
            }
        });
    }
}
