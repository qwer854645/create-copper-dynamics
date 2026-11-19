package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.content.HorizontalSmartTankBlock;
import dev.ccd.content.HorizontalSmartTankBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd")
@PrefixGameTestTemplate(false)
public final class HorizontalTankModelTests {
    @GameTest(template="empty")
    public static void rotatedWindowCornersAndEndCaps(GameTestHelper h) {
        var origin=new BlockPos(1,1,1);
        for(var axis:new Direction.Axis[]{Direction.Axis.X,Direction.Axis.Z}) {
            for(int layer=0;layer<3;layer++) for(int x=0;x<2;x++) for(int z=0;z<2;z++) {
                var pos=origin.offset(axis==Direction.Axis.X?layer:x,axis==Direction.Axis.X?x:z,axis==Direction.Axis.X?z:layer);
                h.setBlock(pos,CopperDynamics.HORIZONTAL_SMART_TANK.get().defaultBlockState().setValue(HorizontalSmartTankBlock.AXIS,axis));
            }
            for(int layer=0;layer<3;layer++) for(int x=0;x<2;x++) for(int z=0;z<2;z++) {
                var pos=origin.offset(axis==Direction.Axis.X?layer:x,axis==Direction.Axis.X?x:z,axis==Direction.Axis.X?z:layer);
                var tank=(HorizontalSmartTankBlockEntity)h.getBlockEntity(pos);
                tank.preventConnectivityUpdate(); tank.setController(h.absolutePos(origin));
                tank.setWidth(2); tank.setHeight(3); tank.setWindows(true); tank.notifyMultiUpdated();
                var state=h.getBlockState(pos);
                int localX=axis==Direction.Axis.X?z:x;
                int localZ=axis==Direction.Axis.X?x:z;
                var expected=localX==0?(localZ==0?FluidTankBlock.Shape.WINDOW_NW:FluidTankBlock.Shape.WINDOW_SW)
                    :(localZ==0?FluidTankBlock.Shape.WINDOW_NE:FluidTankBlock.Shape.WINDOW_SE);
                h.assertTrue(state.getValue(HorizontalSmartTankBlock.SHAPE)==expected,"Rotated window corner mismatch "+axis+" "+pos);
                h.assertTrue(state.getValue(HorizontalSmartTankBlock.BOTTOM)==(layer==(axis==Direction.Axis.Z?2:0)),"Wrong bottom end "+axis);
                h.assertTrue(state.getValue(HorizontalSmartTankBlock.TOP)==(layer==(axis==Direction.Axis.Z?0:2)),"Wrong top end "+axis);
            }
        }
        h.succeed();
    }
}
