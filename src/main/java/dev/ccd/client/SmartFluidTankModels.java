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
                if(sprite != null) {
                    var name=sprite.contents().name().getPath();
                    if(name.endsWith("fluid_tank_inner")) return shift("fluid_tank_inner");
                    if(!name.endsWith("fluid_tank") && !name.endsWith("fluid_tank_top")) return null;
                }
                return face.getAxis()==state.getValue(dev.ccd.content.HorizontalSmartTankBlock.AXIS)?shift("fluid_tank_top"):shift("fluid_tank");
            }
            @Override public boolean connectsTo(net.minecraft.world.level.block.state.BlockState state,
                net.minecraft.world.level.block.state.BlockState other,net.minecraft.world.level.BlockAndTintGetter world,
                net.minecraft.core.BlockPos pos,net.minecraft.core.BlockPos otherPos,net.minecraft.core.Direction face) {
                return state.is(other.getBlock()) && world.getBlockEntity(pos) instanceof dev.ccd.content.HorizontalSmartTankBlockEntity a
                    && world.getBlockEntity(otherPos) instanceof dev.ccd.content.HorizontalSmartTankBlockEntity b
                    && a.getMainConnectionAxis()==b.getMainConnectionAxis() && a.getController().equals(b.getController());
            }
            @Override public CTContext buildContext(net.minecraft.world.level.BlockAndTintGetter world,
                net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state,
                net.minecraft.core.Direction face,ContextRequirement requirement) {
                // CT indices follow the model's UV axes, not world up/right.
                var rotation=state.getValue(dev.ccd.content.HorizontalSmartTankBlock.AXIS)==net.minecraft.core.Direction.Axis.X
                    ?net.minecraft.client.resources.model.BlockModelRotation.X90_Y90
                    :net.minecraft.client.resources.model.BlockModelRotation.X90_Y0;
                var matrix=rotation.getRotation().getMatrix();
                net.minecraft.core.Direction localFace=null;
                for(var candidate:net.minecraft.core.Direction.values())
                    if(net.minecraft.core.Direction.rotate(matrix,candidate)==face) localFace=candidate;
                var up=localFace.getAxis().isHorizontal()?net.minecraft.core.Direction.UP:net.minecraft.core.Direction.NORTH;
                var right=localFace.getAxis()==net.minecraft.core.Direction.Axis.X?net.minecraft.core.Direction.SOUTH:net.minecraft.core.Direction.WEST;
                if(localFace.getAxisDirection()==net.minecraft.core.Direction.AxisDirection.POSITIVE) right=right.getOpposite();
                if(localFace==net.minecraft.core.Direction.DOWN) { up=up.getOpposite(); right=right.getOpposite(); }
                up=net.minecraft.core.Direction.rotate(matrix,up);
                right=net.minecraft.core.Direction.rotate(matrix,right);
                var context=new CTContext();
                var top=pos.relative(up);var bottom=pos.relative(up.getOpposite());
                var left=pos.relative(right.getOpposite());var east=pos.relative(right);
                context.up=connectsTo(state,world.getBlockState(top),world,pos,top,face);
                context.down=connectsTo(state,world.getBlockState(bottom),world,pos,bottom,face);
                context.left=connectsTo(state,world.getBlockState(left),world,pos,left,face);
                context.right=connectsTo(state,world.getBlockState(east),world,pos,east,face);
                return context;
            }
        });
    }
}
