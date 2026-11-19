package dev.ccd.content;

import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Native horizontal connectivity with the existing controller-owned multi-fluid storage. */
public final class HorizontalSmartTankBlockEntity extends SmartFluidTankBlockEntity {
    public HorizontalSmartTankBlockEntity(BlockPos pos,BlockState state) { super(CopperDynamics.HORIZONTAL_SMART_TANK_BE.get(),pos,state); }
    @Override public Direction.Axis getMainConnectionAxis() { return getBlockState().getValue(HorizontalSmartTankBlock.AXIS); }
    @Override public int getMaxLength(Direction.Axis axis,int width) { return axis==getMainConnectionAxis()?getMaxHeight():getMaxWidth(); }
    @Override public int getMaxWidth() { return 3; }
    @Override public BlockPos structureOffset(int x,int y,int z) { return getMainConnectionAxis()==Direction.Axis.X?new BlockPos(y,x,z):new BlockPos(x,z,y); }
    @Override public void setWindows(boolean enabled) {
        window=enabled;
        if(level==null || level.isClientSide()) return;
        for(int y=0;y<height;y++) for(int x=0;x<width;x++) for(int z=0;z<width;z++) {
            var pos=getController().offset(structureOffset(x,y,z));
            if(!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof HorizontalSmartTankBlockEntity part)
                || !getController().equals(part.getController())) continue;
            part.window=enabled;
            part.notifyMultiUpdated();
        }
    }
    @Override public void notifyMultiUpdated() {
        cachedStorage=null; invalidateCapabilities(); invalidateRenderBoundingBox();
        if(level==null || level.isClientSide()) return;
        int coordinate=getMainConnectionAxis().choose(worldPosition.getX(),worldPosition.getY(),worldPosition.getZ());
        int start=getMainConnectionAxis().choose(getController().getX(),getController().getY(),getController().getZ());
        // Shape quadrants are expressed in the upright model's local X/Z plane.
        // The blockstate rotates that plane onto the horizontal cross-section.
        var delta=worldPosition.subtract(getController());
        int localX=getMainConnectionAxis()==Direction.Axis.X?delta.getZ():delta.getX();
        int localZ=delta.getY();
        var shape=com.simibubi.create.content.fluids.tank.FluidTankBlock.Shape.PLAIN;
        if(window) {
            if(width==1) shape=com.simibubi.create.content.fluids.tank.FluidTankBlock.Shape.WINDOW;
            else if(localX==0 && localZ==0) shape=com.simibubi.create.content.fluids.tank.FluidTankBlock.Shape.WINDOW_NW;
            else if(localX==width-1 && localZ==0) shape=com.simibubi.create.content.fluids.tank.FluidTankBlock.Shape.WINDOW_NE;
            else if(localX==0 && localZ==width-1) shape=com.simibubi.create.content.fluids.tank.FluidTankBlock.Shape.WINDOW_SW;
            else if(localX==width-1 && localZ==width-1) shape=com.simibubi.create.content.fluids.tank.FluidTankBlock.Shape.WINDOW_SE;
        }
        // X90 maps local +Y to world -Z; X90/Y90 maps it to world +X.
        boolean reversed=getMainConnectionAxis()==Direction.Axis.Z;
        level.setBlock(worldPosition,getBlockState().setValue(HorizontalSmartTankBlock.BOTTOM,coordinate==(reversed?start+height-1:start))
            .setValue(HorizontalSmartTankBlock.TOP,coordinate==(reversed?start:start+height-1)).setValue(HorizontalSmartTankBlock.SHAPE,shape),6);
        sendData(); setChanged();
    }
    @Override protected AABB createRenderBoundingBox() {
        return new AABB(worldPosition).expandTowards(getMainConnectionAxis()==Direction.Axis.X?height-1:width-1,width-1,getMainConnectionAxis()==Direction.Axis.Z?height-1:width-1);
    }
}
