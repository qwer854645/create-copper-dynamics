package dev.ccd.client.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.ccd.CopperDynamics;
import dev.ccd.content.*;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** Short, deterministic storyboards. State changes are staged, not dependent on live simulation. */
public final class CCDPonderScenes {
    private static final BlockPos A=new BlockPos(1,1,2),B=new BlockPos(2,1,2),C=new BlockPos(3,1,2);
    private static CreateSceneBuilder begin(SceneBuilder builder,String id,String title) {
        var scene=new CreateSceneBuilder(builder);
        scene.title(id,title); scene.configureBasePlate(0,0,5); scene.showBasePlate(); scene.idle(10);
        return scene;
    }
    private static void show(CreateSceneBuilder scene,SceneBuildingUtil util,BlockPos pos,net.minecraft.world.level.block.state.BlockState state) {
        scene.world().setBlock(pos,state,false);
        scene.world().showSection(util.select().position(pos),Direction.DOWN); scene.idle(10);
    }
    private static void text(CreateSceneBuilder scene,SceneBuildingUtil util,BlockPos pos,String message) {
        scene.overlay().showText(65).text(message).pointAt(util.vector().centerOf(pos)).placeNearTarget().attachKeyFrame(); scene.idle(75);
    }
    private static void water(CreateSceneBuilder scene,BlockPos pos,int amount) {
        scene.world().modifyBlockEntity(pos,net.minecraft.world.level.block.entity.BlockEntity.class,entity->{
            var handler=entity.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,pos,null);
            if(handler!=null) { handler.drain(Integer.MAX_VALUE,FluidAction.EXECUTE); if(amount>0) handler.fill(new FluidStack(Fluids.WATER,amount),FluidAction.EXECUTE); }
        });
    }
    public static void packaging(SceneBuilder builder,SceneBuildingUtil util) {
        var s=begin(builder,"fluid_packaging","Packaging and Unpacking Fluids");
        show(s,util,A,AllBlocks.FLUID_TANK.getDefaultState()); water(s,A,4000);
        show(s,util,B,CopperDynamics.FLUID_PACKAGER.get().defaultBlockState().setValue(PackagerBlock.FACING,Direction.EAST));
        text(s,util,A,"The rear of a Fluid Packager connects to a fluid container");
        s.overlay().showOutline(PonderPalette.BLUE,"source",util.select().position(A),45);
        text(s,util,B,"A Redstone Signal packs up to 4000 mB into a Copper Package");
        s.world().modifyBlockEntity(B,FluidPackagerBlockEntity.class,be->{be.heldBox=FluidPackageItem.containingFluid(new FluidStack(Fluids.WATER,4000));}); water(s,A,0);
        text(s,util,B,"Copper Packages use the same addresses and transport as ordinary Packages");
        s.world().modifyBlockEntity(B,FluidPackagerBlockEntity.class,be->{be.heldBox=ItemStack.EMPTY;}); water(s,A,4000);
        text(s,util,A,"Arriving Copper Packages are emptied into the attached container");
        s.overlay().showControls(util.vector().centerOf(B),Pointing.DOWN,45).rightClick().withItem(new ItemStack(Items.WATER_BUCKET));
        text(s,util,B,"R-Click with a filled container to filter Redstone-triggered packaging");
        show(s,util,C,AllBlocks.REPACKAGER.getDefaultState());
        text(s,util,C,"A Repackager combines items and fluids from the same order into Brass Packages"); s.markAsFinished();
    }
    public static void hatches(SceneBuilder builder,SceneBuildingUtil util) {
        var s=begin(builder,"fluid_hatches","Accessing Fluids using Fluid Hatches");
        show(s,util,A,CopperDynamics.SMART_FLUID_TANK.get().defaultBlockState()); water(s,A,3000);
        show(s,util,B,CopperDynamics.FLUID_HATCH.get().defaultBlockState().setValue(FluidHatchBlock.FACING,Direction.WEST));
        text(s,util,B,"Fluid Hatches provide access to fluids in the container behind them");
        s.overlay().showControls(util.vector().centerOf(B),Pointing.DOWN,45).rightClick().withItem(new ItemStack(Items.BUCKET));
        text(s,util,B,"R-Click the front with a fluid container to empty or fill it");
        s.world().setBlock(B,CopperDynamics.SMART_FLUID_HATCH.get().defaultBlockState().setValue(FluidHatchBlock.FACING,Direction.WEST),true);
        s.overlay().showFilterSlotInput(util.vector().centerOf(B),Direction.EAST,45);
        text(s,util,B,"Smart Fluid Hatches only allow fluids selected by their filter");
        s.world().setFilterData(util.select().position(B),SmartFluidHatchBlockEntity.class,new ItemStack(Items.WATER_BUCKET));
        var arm=new BlockPos(2,1,4);
        show(s,util,arm,CopperDynamics.FLUID_ARM.get().defaultBlockState());
        s.overlay().showControls(util.vector().centerOf(B),Pointing.DOWN,45).rightClick().withItem(new ItemStack(CopperDynamics.FLUID_ARM_ITEM.get()));
        text(s,util,B,"Before placing a Mechanical Fluid Arm, select hatch fronts as inputs and outputs");
        var output=new BlockPos(4,1,3); var hatch=output.south();
        show(s,util,output,AllBlocks.FLUID_TANK.getDefaultState());
        show(s,util,hatch,CopperDynamics.FLUID_HATCH.get().defaultBlockState().setValue(FluidHatchBlock.FACING,Direction.NORTH));
        s.overlay().showOutline(PonderPalette.BLUE,"input",util.select().position(B),60);
        s.overlay().showOutline(PonderPalette.GREEN,"output",util.select().position(hatch),60);
        text(s,util,arm,"Supply rotational power to move fluids between the selected locations"); s.markAsFinished();
    }
    public static void access(SceneBuilder builder,SceneBuildingUtil util) {
        var s=begin(builder,"fluid_access","Extending Fluid Container Connections");
        show(s,util,A,AllBlocks.FLUID_TANK.getDefaultState()); water(s,A,2000);
        show(s,util,B,CopperDynamics.FLUID_ACCESS_PORT.get().defaultBlockState().setValue(FluidAccessBlock.FACING,Direction.EAST));
        text(s,util,B,"All faces of a Fluid Access Port share access to its attached container");
        s.overlay().showOutline(PonderPalette.RED,"blocked",util.select().position(C),45);
        text(s,util,C,"Fluid Access Ports and Fluid Bridges cannot extend each other");
        s.world().setBlock(B,CopperDynamics.FLUID_BRIDGE.get().defaultBlockState().setValue(FluidAccessBlock.FACING,Direction.EAST),true);
        show(s,util,C,AllBlocks.FLUID_TANK.getDefaultState()); water(s,C,2000);
        text(s,util,B,"A Fluid Bridge provides access to both end containers through one interface");
        show(s,util,B.south(),AllBlocks.FLUID_PIPE.getDefaultState());
        s.overlay().showFilterSlotInput(util.vector().centerOf(B),Direction.EAST,45);
        text(s,util,B,"Fluids selected by one end are excluded from the opposite end if its filter is empty"); s.markAsFinished();
    }
    public static void tanks(SceneBuilder builder,SceneBuildingUtil util) {
        var s=begin(builder,"smart_tanks","Storage using Smart Fluid Tanks");
        show(s,util,B,CopperDynamics.SMART_FLUID_TANK.get().defaultBlockState());
        show(s,util,B.above(),CopperDynamics.SMART_FLUID_TANK.get().defaultBlockState());
        text(s,util,B,"Smart Fluid Tanks can be stacked for greater storage capacity");
        text(s,util,B.above(),"Their square cross-section can be 1×1, 2×2 or 3×3 blocks");
        s.world().hideSection(util.select().fromTo(B,B.above()),Direction.UP); s.idle(20);
        for(var p:java.util.List.of(A,B,C)) show(s,util,p,CopperDynamics.HORIZONTAL_SMART_TANK.get().defaultBlockState().setValue(HorizontalSmartTankBlock.AXIS,Direction.Axis.X));
        text(s,util,B,"Horizontal Smart Fluid Tanks extend along their placement axis");
        water(s,A,3000);
        text(s,util,A,"Different fluids remain separate while sharing the structure’s capacity");
        s.overlay().showControls(util.vector().centerOf(B),Pointing.DOWN,45).rightClick().withItem(com.simibubi.create.AllItems.WRENCH.asStack());
        text(s,util,B,"A Wrench can be used to toggle the windows"); s.markAsFinished();
    }
    public static void faucets(SceneBuilder builder,SceneBuildingUtil util) {
        var s=begin(builder,"fluid_faucets","Pouring Fluids using Faucets");
        var source=new BlockPos(2,3,1); var faucet=new BlockPos(2,3,2); var target=faucet.below();
        show(s,util,source,AllBlocks.FLUID_TANK.getDefaultState()); water(s,source,4000);
        show(s,util,faucet,CopperDynamics.FAUCET.get().defaultBlockState().setValue(FaucetBlock.FACING,Direction.SOUTH));
        show(s,util,target,Blocks.CAULDRON.defaultBlockState());
        text(s,util,faucet,"Faucets attach to the side of a fluid container");
        s.overlay().showControls(util.vector().centerOf(faucet),Pointing.DOWN,45).rightClick();
        s.world().modifyBlock(faucet,state->state.setValue(FaucetBlock.OPEN,true),false);
        s.world().setBlock(target,Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL,3),true);
        text(s,util,target,"Open the Faucet to fill a container directly below");
        s.world().setBlock(faucet,CopperDynamics.SMART_FAUCET.get().defaultBlockState().setValue(FaucetBlock.FACING,Direction.SOUTH),true);
        s.world().setBlock(target,Blocks.AIR.defaultBlockState(),false);
        show(s,util,faucet.below(2),AllBlocks.DEPOT.getDefaultState());
        s.world().createItemOnBeltLike(faucet.below(2),Direction.NORTH,new ItemStack(Items.GLASS_BOTTLE));
        text(s,util,faucet.below(2),"Smart Faucets can also fill items on Belts or Depots two blocks below"); s.markAsFinished();
    }
    public static void atomizing(SceneBuilder builder,SceneBuildingUtil util) {
        var s=begin(builder,"fluid_atomizing","Bulk Processing using Fluid Atomizers");
        show(s,util,B,CopperDynamics.FLUID_ATOMIZER.get().defaultBlockState().setValue(FluidAtomizerBlock.FACING,Direction.SOUTH));
        text(s,util,B,"Supply rotation at the rear and connect Fluid Pipes at the sides");
        s.overlay().showControls(util.vector().centerOf(B),Pointing.DOWN,45).rightClick().withItem(new ItemStack(Items.WATER_BUCKET));
        water(s,B,3000); s.world().setKineticSpeed(util.select().position(B),32);
        text(s,util,B,"Handheld fluid containers can also be emptied or filled by R-Clicking");
        var depot=new BlockPos(2,1,4); show(s,util,depot,AllBlocks.DEPOT.getDefaultState());
        s.world().createItemOnBeltLike(depot,Direction.NORTH,com.simibubi.create.AllItems.CRUSHED_IRON.asStack());
        text(s,util,depot,"The stored fluid determines the processing effect of the air current");
        s.world().modifyBlock(B,state->state.setValue(FluidAtomizerBlock.POWERED,true),false);
        text(s,util,B,"A Redstone Signal stops atomizing without stopping the air current"); s.markAsFinished();
    }
    public static void valves(SceneBuilder builder,SceneBuildingUtil util) {
        var s=begin(builder,"fluid_switching","Switching Fluid Pipe Connections");
        show(s,util,B,CopperDynamics.THREE_WAY_VALVE.get().defaultBlockState());
        show(s,util,B.north(),AllBlocks.FLUID_PIPE.getDefaultState());
        show(s,util,B.south(),AllBlocks.FLUID_PIPE.getDefaultState());
        show(s,util,B.above(),AllBlocks.FLUID_PIPE.getDefaultState());
        s.overlay().showOutline(PonderPalette.BLUE,"common",util.select().position(B.north()),55);
        text(s,util,B,"The silver connector is shared by both connections");
        text(s,util,B,"Without a Redstone Signal, it connects to the opposite end");
        s.overlay().showOutline(PonderPalette.GREEN,"route",util.select().fromTo(B,B.south()),55);
        s.world().modifyBlock(B,state->state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED,true),false);
        text(s,util,B,"A Redstone Signal switches it to the side connection");
        s.overlay().showOutline(PonderPalette.GREEN,"route",util.select().fromTo(B,B.above()),55);
        text(s,util,B,"Mechanical Pumps still determine the direction of flow"); s.markAsFinished();
    }
    public static void collection(SceneBuilder builder,SceneBuildingUtil util) {
        var s=begin(builder,"fluid_collection","Collecting Fluids and Supplying Water");
        show(s,util,B,AllBlocks.FLUID_TANK.getDefaultState());
        var grate=B.above();
        show(s,util,grate,CopperDynamics.COPPER_COLLECTION_GRATE.get().defaultBlockState());
        show(s,util,grate.above(),Blocks.WATER.defaultBlockState());
        text(s,util,grate,"Collection Grates collect fluids touching their top face");
        s.world().setBlock(grate.above(),Blocks.AIR.defaultBlockState(),false); water(s,B,1000);
        text(s,util,B,"Collected fluids drain into a container directly below");
        s.world().setBlock(grate,CopperDynamics.SMART_COLLECTION_GRATE.get().defaultBlockState(),true);
        text(s,util,grate,"Smart Collection Grates can filter fluids; Redstone pauses collection and draining");
        s.world().hideSection(util.select().fromTo(B,grate),Direction.UP); s.idle(20);
        s.world().setBlock(B,CopperDynamics.COPPER_WATER_SUPPLY.get().defaultBlockState(),false);
        s.world().showSection(util.select().position(B),Direction.DOWN);
        show(s,util,C,AllBlocks.FLUID_TANK.getDefaultState());
        text(s,util,B,"Water Supply Boxes automatically supply water to adjacent containers on all six sides");
        water(s,C,4000);
        s.world().setBlock(B,CopperDynamics.COPPER_WATER_BASIN.get().defaultBlockState(),true);
        text(s,util,B,"Copper Water Basins supply handheld containers and pipes, but do not fill adjacent containers automatically"); s.markAsFinished();
    }
    public static void transfer(SceneBuilder builder,SceneBuildingUtil util) {
        var s=begin(builder,"fluid_transfer","Fluid Transportation using Fluid Transmitters");
        var source=new BlockPos(2,1,1); var target=new BlockPos(2,1,3);
        show(s,util,source,AllBlocks.FLUID_TANK.getDefaultState()); water(s,source,3000);
        show(s,util,B,CopperDynamics.FLUID_TRANSMITTER.get().defaultBlockState().setValue(FluidTransmitterBlock.FACING,Direction.SOUTH));
        show(s,util,target,AllBlocks.FLUID_TANK.getDefaultState());
        text(s,util,B,"Fluid Transmitters move fluids between end containers without rotational power");
        s.overlay().showOutline(PonderPalette.BLUE,"input",util.select().position(source),55);
        s.overlay().showOutline(PonderPalette.GREEN,"output",util.select().position(target),55);
        water(s,source,2000); water(s,target,1000);
        text(s,util,B,"The arrow indicates the direction of flow");
        s.overlay().showControls(util.vector().centerOf(B),Pointing.DOWN,45).rightClick().withItem(com.simibubi.create.AllItems.WRENCH.asStack());
        s.world().modifyBlock(B,state->state.setValue(FluidTransmitterBlock.FACING,Direction.NORTH),false);
        text(s,util,B,"A Wrench can be used to reverse the direction");
        s.world().modifyBlock(B,state->state.setValue(FluidTransmitterBlock.POWERED,true),false);
        text(s,util,B,"A Redstone Signal pauses the transfer"); s.markAsFinished();
    }
    public static void pumping(SceneBuilder builder,SceneBuildingUtil util) {
        var s=begin(builder,"booster_pumping","Fluid Transportation using Booster Pumps");
        var source=new BlockPos(2,1,0); var target=new BlockPos(2,1,4);
        show(s,util,source,AllBlocks.FLUID_TANK.getDefaultState()); water(s,source,4000);
        show(s,util,new BlockPos(2,1,1),AllBlocks.FLUID_PIPE.getDefaultState());
        show(s,util,B,CopperDynamics.BOOSTER_PUMP.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,Direction.SOUTH));
        show(s,util,new BlockPos(2,1,3),AllBlocks.FLUID_PIPE.getDefaultState());
        show(s,util,target,AllBlocks.FLUID_TANK.getDefaultState());
        s.world().setKineticSpeed(util.select().position(B),32);
        text(s,util,B,"Booster Pumps govern the flow of their attached pipe networks");
        water(s,source,2000); water(s,target,2000);
        text(s,util,B,"The direction marker indicates the direction of flow");
        s.overlay().showControls(util.vector().centerOf(B),Pointing.DOWN,45).rightClick().withItem(com.simibubi.create.AllItems.WRENCH.asStack());
        s.world().modifyBlock(B,state->state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,Direction.NORTH),false);
        text(s,util,B,"Use a Wrench to reverse the flow; input rotation direction does not affect it");
        text(s,util,B,"Greater pumping pressure and range come with a higher Stress Impact"); s.markAsFinished();
    }
}
