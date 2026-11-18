package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.content.FluidAccessBlock;
import dev.ccd.content.FluidAccessBlockEntity;
import dev.ccd.logistics.FluidAccess;
import dev.ccd.logistics.FluidStockScope;
import dev.ccd.content.FluidPackagerBlockEntity;
import dev.ccd.content.FluidStockItem;
import com.simibubi.create.content.logistics.packager.PackagerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CopperDynamics.ID)
@PrefixGameTestTemplate(false)
public final class FluidAccessTests {
    @GameTest(template="empty")
    public static void placementFindsContainerInsteadOfFollowingClickedFace(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);
        tank(h,pos.west(),1000);
        var context=new net.minecraft.world.item.context.DirectionalPlaceContext(h.getLevel(),h.absolutePos(pos),
            Direction.DOWN,new ItemStack(CopperDynamics.FLUID_ACCESS_PORT_ITEM.get()),Direction.UP);
        var port=CopperDynamics.FLUID_ACCESS_PORT.get().getStateForPlacement(context);
        var bridge=CopperDynamics.FLUID_BRIDGE.get().getStateForPlacement(context);
        h.assertTrue(port.getValue(FluidAccessBlock.FACING)==Direction.EAST,"Port ignored neighbour in favour of clicked face");
        h.assertTrue(bridge.getValue(FluidAccessBlock.FACING).getAxis()==Direction.Axis.X,"Bridge did not choose container axis");
        h.setBlock(pos.west(),net.minecraft.world.level.block.Blocks.AIR);
        tank(h,pos.above(),1000);
        h.assertTrue(CopperDynamics.FLUID_ACCESS_PORT.get().getStateForPlacement(context).getValue(FluidAccessBlock.FACING)==Direction.DOWN,
            "Port failed ceiling connection");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void placementFallbackRespectsSneaking(GameTestHelper h) {
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        var pos=h.absolutePos(new BlockPos(2,2,2));
        player.setYRot(0); player.setXRot(0);
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),Direction.UP,pos,false);
        var context=new net.minecraft.world.item.context.BlockPlaceContext(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND,
            new ItemStack(CopperDynamics.FLUID_ACCESS_PORT_ITEM.get()),hit);
        var normal=CopperDynamics.FLUID_ACCESS_PORT.get().getStateForPlacement(context);
        player.setShiftKeyDown(true);
        var sneaking=CopperDynamics.FLUID_ACCESS_PORT.get().getStateForPlacement(context);
        h.assertTrue(normal.getValue(FluidAccessBlock.FACING)==sneaking.getValue(FluidAccessBlock.FACING).getOpposite(),"Sneaking did not reverse fallback target");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void wrenchAndComparatorFollowConnectedBehaviour(GameTestHelper h) {
        var port=CopperDynamics.FLUID_ACCESS_PORT.get();
        var state=port.defaultBlockState().setValue(FluidAccessBlock.FACING,Direction.NORTH);
        h.assertTrue(port.getRotatedBlockState(state,Direction.EAST).getValue(FluidAccessBlock.FACING)
            !=port.getRotatedBlockState(state,Direction.WEST).getValue(FluidAccessBlock.FACING),"Opposite wrench faces rotated identically");
        var pos=new BlockPos(2,2,2);
        tank(h,pos.west(),4000);
        h.setBlock(pos,port.defaultBlockState().setValue(FluidAccessBlock.FACING,Direction.EAST));
        int source=h.getBlockState(pos.west()).getAnalogOutputSignal(h.getLevel(),h.absolutePos(pos.west()));
        h.assertTrue(source>0,"Source comparator output unavailable");
        h.assertTrue(port.getAnalogOutputSignal(h.getBlockState(pos),h.getLevel(),h.absolutePos(pos))==source,"Port did not forward comparator output");
        h.setBlock(pos,CopperDynamics.FLUID_BRIDGE.get().defaultBlockState().setValue(FluidAccessBlock.FACING,Direction.EAST));
        h.assertTrue(CopperDynamics.FLUID_BRIDGE.get().getAnalogOutputSignal(h.getBlockState(pos),h.getLevel(),h.absolutePos(pos))==source/2,
            "Single-ended bridge did not halve comparator output");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void bridgeFillsDirectContainerWhenPortSourceRejects(GameTestHelper h) {
        // Two different fluid types: the working port's lava source cannot receive water.
        var direct=new BlockPos(1,2,2); var bridgePos=new BlockPos(2,2,2); var portPos=new BlockPos(3,2,2); var sourcePos=new BlockPos(4,2,2);
        tank(h,direct,0);
        h.setBlock(sourcePos,com.simibubi.create.AllBlocks.FLUID_TANK.get());
        var source=h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,h.absolutePos(sourcePos),Direction.WEST);
        source.fill(new FluidStack(Fluids.LAVA,1000),FluidAction.EXECUTE);
        h.setBlock(portPos,CopperDynamics.FLUID_ACCESS_PORT.get().defaultBlockState().setValue(FluidAccessBlock.FACING,Direction.WEST));
        h.setBlock(bridgePos,CopperDynamics.FLUID_BRIDGE.get().defaultBlockState().setValue(FluidAccessBlock.FACING,Direction.EAST));
        var handler=h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,h.absolutePos(bridgePos),Direction.NORTH);
        h.assertTrue(handler.fill(water(1000),FluidAction.SIMULATE)==1000,"Port source blocked direct container simulation");
        h.assertTrue(handler.fill(water(1000),FluidAction.EXECUTE)==1000,"Port source blocked direct container execution");
        h.assertTrue(h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,h.absolutePos(direct),Direction.EAST).drain(water(1000),FluidAction.SIMULATE).getAmount()==1000,"Direct container received no water");
        h.succeed();
    }
    private static FluidStack water(int amount) { return new FluidStack(Fluids.WATER,amount); }
    private static void tank(GameTestHelper h,BlockPos pos,int amount) {
        h.setBlock(pos,CopperDynamics.SMART_FLUID_TANK.get());
        var handler=h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,h.absolutePos(pos),Direction.NORTH);
        h.assertTrue(handler!=null && handler.fill(water(amount),FluidAction.EXECUTE)==amount,"Cannot initialize tank");
    }
    @GameTest(template="empty")
    public static void portsCannotExtendPorts(GameTestHelper h) {
        tank(h,new BlockPos(1,2,1),2000);
        for(int z=2;z<=3;z++) h.setBlock(new BlockPos(1,2,z),CopperDynamics.FLUID_ACCESS_PORT.get().defaultBlockState().setValue(FluidAccessBlock.FACING,Direction.SOUTH));
        var pos=h.absolutePos(new BlockPos(1,2,3));
        var handler=h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,pos,Direction.EAST);
        h.assertTrue(handler!=null && handler.getTanks()==0,"Port must not extend another port");
        var direct=h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,h.absolutePos(new BlockPos(1,2,2)),Direction.EAST);
        h.assertTrue(direct.drain(700,FluidAction.SIMULATE).getAmount()==700,"Direct port must expose source");
        h.assertTrue(direct.getFluidInTank(0).getAmount()==2000,"Simulation consumed fluid");
        h.assertTrue(direct.drain(700,FluidAction.EXECUTE).getAmount()==700,"Port transfer failed");
        h.setBlock(new BlockPos(1,2,2),net.minecraft.world.level.block.Blocks.AIR);
        h.assertTrue(handler.getTanks()==0 && handler.drain(100,FluidAction.EXECUTE).isEmpty(),"Stale chain retained access");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void bridgeFiltersAndIndependentSources(GameTestHelper h) {
        tank(h,new BlockPos(1,2,2),1000); tank(h,new BlockPos(3,2,2),1500);
        var pos=new BlockPos(2,2,2);
        h.setBlock(pos,CopperDynamics.FLUID_BRIDGE.get().defaultBlockState().setValue(FluidAccessBlock.FACING,Direction.EAST));
        var bridge=(FluidAccessBlockEntity)h.getBlockEntity(pos);
        bridge.initialize();
        bridge.filters.setFilter(Direction.WEST,new ItemStack(Items.LAVA_BUCKET));
        var handler=bridge.handler(Direction.NORTH);
        h.assertTrue(bridge.handler(Direction.WEST).getTanks()==handler.getTanks(),"Bridge must expose the same inventory on every face");
        h.assertTrue(handler.drain(water(2000),FluidAction.SIMULATE).getAmount()==1500,"Filter exposed rejected source");
        h.assertTrue(handler.drain(water(2000),FluidAction.EXECUTE).getAmount()==1500,"Filtered drain failed");
        bridge.filters.setFilter(Direction.WEST,ItemStack.EMPTY);
        h.assertTrue(handler.drain(water(2000),FluidAction.EXECUTE).getAmount()==1000,"Filter change did not apply");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void loopsAreUnavailable(GameTestHelper h) {
        h.setBlock(new BlockPos(1,2,1),CopperDynamics.FLUID_ACCESS_PORT.get().defaultBlockState().setValue(FluidAccessBlock.FACING,Direction.NORTH));
        h.setBlock(new BlockPos(1,2,2),CopperDynamics.FLUID_ACCESS_PORT.get().defaultBlockState().setValue(FluidAccessBlock.FACING,Direction.SOUTH));
        h.assertTrue(FluidAccess.resolve(h.getLevel(),h.absolutePos(new BlockPos(1,2,1)),Direction.EAST).isEmpty(),"Loop resolved to storage");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void explicitFilterReservesFluidEvenWhenItsContainerIsMissing(GameTestHelper h) {
        tank(h,new BlockPos(3,2,2),1000);
        var pos=new BlockPos(2,2,2);
        h.setBlock(pos,CopperDynamics.FLUID_BRIDGE.get().defaultBlockState().setValue(FluidAccessBlock.FACING,Direction.EAST));
        var bridge=(FluidAccessBlockEntity)h.getBlockEntity(pos);
        bridge.initialize();
        var handler=bridge.handler(Direction.UP);
        bridge.filters.setFilter(Direction.WEST,new ItemStack(Items.WATER_BUCKET));
        h.assertTrue(handler.drain(1000,FluidAction.SIMULATE).isEmpty(),"Empty-filter end leaked reserved water");
        h.assertTrue(handler.fill(water(1000),FluidAction.SIMULATE)==0,"Empty-filter end accepted reserved water");
        bridge.filters.setFilter(Direction.WEST,ItemStack.EMPTY);
        h.assertTrue(handler.drain(1000,FluidAction.EXECUTE).getAmount()==1000,"Clearing filter did not restore access");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void powerDisconnectsExistingHandlerAndReconnects(GameTestHelper h) {
        tank(h,new BlockPos(1,2,2),1000);
        var pos=new BlockPos(2,2,2);
        h.setBlock(pos,CopperDynamics.FLUID_ACCESS_PORT.get().defaultBlockState().setValue(FluidAccessBlock.FACING,Direction.EAST));
        var port=(FluidAccessBlockEntity)h.getBlockEntity(pos);
        port.initialize();
        var handler=port.handler(Direction.WEST);
        h.assertTrue(handler.getTanks()>0,"Port connection unavailable");
        h.setBlock(pos.above(),net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
        ((FluidAccessBlock)port.getBlockState().getBlock()).refreshConnections(h.getLevel(),h.absolutePos(pos));
        h.assertTrue(handler.getTanks()==0 && handler.drain(1000,FluidAction.EXECUTE).isEmpty(),"Powered stale handler retained access");
        h.assertTrue(!port.getBlockState().getValue(FluidAccessBlock.ATTACHED_NEGATIVE),"Powered connection still attached");
        h.setBlock(pos.above(),net.minecraft.world.level.block.Blocks.AIR);
        ((FluidAccessBlock)port.getBlockState().getBlock()).refreshConnections(h.getLevel(),h.absolutePos(pos));
        h.assertTrue(handler.drain(1000,FluidAction.SIMULATE).getAmount()==1000,"Unpowered port did not reconnect");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void bridgeCannotBeExtendedByPort(GameTestHelper h) {
        tank(h,new BlockPos(1,2,2),1000);
        h.setBlock(new BlockPos(2,2,2),CopperDynamics.FLUID_BRIDGE.get().defaultBlockState().setValue(FluidAccessBlock.FACING,Direction.EAST));
        var pos=new BlockPos(2,2,3);
        h.setBlock(pos,CopperDynamics.FLUID_ACCESS_PORT.get().defaultBlockState().setValue(FluidAccessBlock.FACING,Direction.SOUTH));
        var port=(FluidAccessBlockEntity)h.getBlockEntity(pos);
        h.assertTrue(port.handler(null).getTanks()==0,"Port extended bridge inventory");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void overlappingStockCountsOnce(GameTestHelper h) {
        tank(h,new BlockPos(1,2,2),1000); tank(h,new BlockPos(3,2,2),1500);
        h.setBlock(new BlockPos(2,2,2),CopperDynamics.FLUID_BRIDGE.get().defaultBlockState().setValue(FluidAccessBlock.FACING,Direction.EAST));
        h.setBlock(new BlockPos(2,2,3),CopperDynamics.FLUID_PACKAGER.get().defaultBlockState().setValue(PackagerBlock.FACING,Direction.SOUTH));
        h.setBlock(new BlockPos(1,2,3),CopperDynamics.FLUID_PACKAGER.get().defaultBlockState().setValue(PackagerBlock.FACING,Direction.SOUTH));
        var bridge=(FluidPackagerBlockEntity)h.getBlockEntity(new BlockPos(2,2,3));
        var direct=(FluidPackagerBlockEntity)h.getBlockEntity(new BlockPos(1,2,3));
        try(var scope=new FluidStockScope(null)) {
            int first=direct.getAvailableItems().getCountOf(FluidStockItem.of(water(1)));
            int second=bridge.getAvailableItems().getCountOf(FluidStockItem.of(water(1)));
            h.assertTrue(first+second==2500,"Overlapping bridge double-counted stock");
            h.assertTrue(bridge.getAvailableItems().getCountOf(FluidStockItem.of(water(1)))==second,"Repeated reads changed reservation");
        }
        h.succeed();
    }
}
