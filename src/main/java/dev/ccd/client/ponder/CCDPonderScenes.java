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
        var s=begin(builder,"fluid_packaging","Packaging and unpacking fluids");
        show(s,util,A,AllBlocks.FLUID_TANK.getDefaultState()); water(s,A,4000);
        show(s,util,B,CopperDynamics.FLUID_PACKAGER.get().defaultBlockState().setValue(PackagerBlock.FACING,Direction.EAST));
        text(s,util,A,"Attach the rear of a Fluid Packager to a fluid container");
        s.overlay().showOutline(PonderPalette.BLUE,"source",util.select().position(A),45);
        text(s,util,B,"A redstone signal packs up to 4000 mB into a Copper Package");
        s.world().modifyBlockEntity(B,FluidPackagerBlockEntity.class,be->{be.heldBox=FluidPackageItem.containingFluid(new FluidStack(Fluids.WATER,4000));}); water(s,A,0);
        text(s,util,B,"Packages use the same addresses and transport as ordinary packages");
        s.world().modifyBlockEntity(B,FluidPackagerBlockEntity.class,be->{be.heldBox=ItemStack.EMPTY;}); water(s,A,4000);
        text(s,util,A,"Arriving Copper Packages empty into the container behind");
        s.overlay().showControls(util.vector().centerOf(B),Pointing.DOWN,45).rightClick().withItem(new ItemStack(Items.WATER_BUCKET));
        text(s,util,B,"Use a filled container to set the filter for redstone packaging");
        show(s,util,C,AllBlocks.REPACKAGER.getDefaultState());
        text(s,util,C,"A Repackager combines items and fluids from the same order into Brass Packages"); s.markAsFinished();
    }
    public static void hatches(SceneBuilder builder,SceneBuildingUtil util) {
        var s=begin(builder,"fluid_hatches","Accessing fluids through hatches");
        show(s,util,A,CopperDynamics.SMART_FLUID_TANK.get().defaultBlockState()); water(s,A,3000);
        show(s,util,B,CopperDynamics.FLUID_HATCH.get().defaultBlockState().setValue(FluidHatchBlock.FACING,Direction.WEST));
        text(s,util,B,"Fluid Hatches provide access to the container behind");
        s.overlay().showControls(util.vector().centerOf(B),Pointing.DOWN,45).rightClick().withItem(new ItemStack(Items.BUCKET));
        text(s,util,B,"Right-click the exposed front with a fluid container to insert or extract fluid");
        s.world().setBlock(B,CopperDynamics.SMART_FLUID_HATCH.get().defaultBlockState().setValue(FluidHatchBlock.FACING,Direction.WEST),true);
        s.overlay().showFilterSlotInput(util.vector().centerOf(B),Direction.EAST,45);
        text(s,util,B,"Smart Fluid Hatches only allow the selected fluid to pass");
        s.world().setFilterData(util.select().position(B),SmartFluidHatchBlockEntity.class,new ItemStack(Items.WATER_BUCKET));
        var arm=new BlockPos(2,1,4);
        show(s,util,arm,CopperDynamics.FLUID_ARM.get().defaultBlockState());
        s.overlay().showControls(util.vector().centerOf(B),Pointing.DOWN,45).rightClick().withItem(new ItemStack(CopperDynamics.FLUID_ARM_ITEM.get()));
        text(s,util,B,"Before placing an arm, select hatch fronts as fluid inputs and outputs");
        var output=new BlockPos(4,1,3); var hatch=output.south();
        show(s,util,output,AllBlocks.FLUID_TANK.getDefaultState());
        show(s,util,hatch,CopperDynamics.FLUID_HATCH.get().defaultBlockState().setValue(FluidHatchBlock.FACING,Direction.NORTH));
        s.overlay().showOutline(PonderPalette.BLUE,"input",util.select().position(B),60);
        s.overlay().showOutline(PonderPalette.GREEN,"output",util.select().position(hatch),60);
        text(s,util,arm,"Supply rotation after placing the arm to move fluid between the selected points"); s.markAsFinished();
    }
    public static void access(SceneBuilder builder,SceneBuildingUtil util) {
        var s=begin(builder,"fluid_access","Extending and combining fluid access");
        show(s,util,A,AllBlocks.FLUID_TANK.getDefaultState()); water(s,A,2000);
        show(s,util,B,CopperDynamics.FLUID_ACCESS_PORT.get().defaultBlockState().setValue(FluidAccessBlock.FACING,Direction.EAST));
        text(s,util,B,"An Access Port exposes the attached container on its other five faces");
        s.overlay().showOutline(PonderPalette.RED,"blocked",util.select().position(C),45);
        text(s,util,C,"Access Ports cannot extend other Access Ports");
        s.world().setBlock(B,CopperDynamics.FLUID_BRIDGE.get().defaultBlockState().setValue(FluidAccessBlock.FACING,Direction.EAST),true);
        show(s,util,C,AllBlocks.FLUID_TANK.getDefaultState()); water(s,C,2000);
        text(s,util,B,"A Fluid Bridge gives side connections access to both end containers");
        show(s,util,B.south(),AllBlocks.FLUID_PIPE.getDefaultState());
        s.overlay().showFilterSlotInput(util.vector().centerOf(B),Direction.EAST,45);
        text(s,util,B,"Each end can filter which fluids enter or leave its container"); s.markAsFinished();
    }
    public static void tanks(SceneBuilder builder,SceneBuildingUtil util) {
        var s=begin(builder,"smart_tanks","Combining Smart Fluid Tanks");
        show(s,util,B,CopperDynamics.SMART_FLUID_TANK.get().defaultBlockState());
        show(s,util,B.above(),CopperDynamics.SMART_FLUID_TANK.get().defaultBlockState());
        text(s,util,B,"Smart Fluid Tanks combine into structures with shared capacity");
        text(s,util,B.above(),"The square cross-section can be one, two or three blocks wide");
        s.world().hideSection(util.select().fromTo(B,B.above()),Direction.UP); s.idle(20);
        for(var p:java.util.List.of(A,B,C)) show(s,util,p,CopperDynamics.HORIZONTAL_SMART_TANK.get().defaultBlockState().setValue(HorizontalSmartTankBlock.AXIS,Direction.Axis.X));
        text(s,util,B,"Horizontal Smart Fluid Tanks extend along their placement axis");
        water(s,A,3000);
        text(s,util,A,"Different fluids remain separate while sharing the structure's capacity");
        s.overlay().showControls(util.vector().centerOf(B),Pointing.DOWN,45).rightClick().withItem(com.simibubi.create.AllItems.WRENCH.asStack());
        text(s,util,B,"Right-click with a Wrench to toggle the windows"); s.markAsFinished();
    }
    public static void faucets(SceneBuilder builder,SceneBuildingUtil util) {
        var s=begin(builder,"fluid_faucets","Pouring and filling with faucets");
        var source=new BlockPos(2,3,1); var faucet=new BlockPos(2,3,2); var target=faucet.below();
        show(s,util,source,AllBlocks.FLUID_TANK.getDefaultState()); water(s,source,4000);
        show(s,util,faucet,CopperDynamics.FAUCET.get().defaultBlockState().setValue(FaucetBlock.FACING,Direction.SOUTH));
        show(s,util,target,Blocks.CAULDRON.defaultBlockState());
        text(s,util,faucet,"Attach a faucet to the side of a fluid container");
        s.overlay().showControls(util.vector().centerOf(faucet),Pointing.DOWN,45).rightClick();
        s.world().modifyBlock(faucet,state->state.setValue(FaucetBlock.OPEN,true),false);
        s.world().setBlock(target,Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL,3),true);
        text(s,util,target,"Open the faucet to fill the container directly below");
        s.world().setBlock(faucet,CopperDynamics.SMART_FAUCET.get().defaultBlockState().setValue(FaucetBlock.FACING,Direction.SOUTH),true);
        s.world().setBlock(target,Blocks.AIR.defaultBlockState(),false);
        show(s,util,faucet.below(2),AllBlocks.DEPOT.getDefaultState());
        s.world().createItemOnBeltLike(faucet.below(2),Direction.NORTH,new ItemStack(Items.GLASS_BOTTLE));
        text(s,util,faucet.below(2),"Smart Faucets fill items on a Belt or Depot two blocks below"); s.markAsFinished();
    }
    public static void atomizing(SceneBuilder builder,SceneBuildingUtil util) {
        var s=begin(builder,"fluid_atomizing","Processing with fluid spray");
        show(s,util,B,CopperDynamics.FLUID_ATOMIZER.get().defaultBlockState().setValue(FluidAtomizerBlock.FACING,Direction.SOUTH));
        text(s,util,B,"Supply rotation at the rear and fluid through the sides");
        s.overlay().showControls(util.vector().centerOf(B),Pointing.DOWN,45).rightClick().withItem(new ItemStack(Items.WATER_BUCKET));
        water(s,B,3000); s.world().setKineticSpeed(util.select().position(B),32);
        text(s,util,B,"Fluid containers can also fill or empty the Atomizer by hand");
        var depot=new BlockPos(2,1,4); show(s,util,depot,AllBlocks.DEPOT.getDefaultState());
        s.world().createItemOnBeltLike(depot,Direction.NORTH,com.simibubi.create.AllItems.CRUSHED_IRON.asStack());
        text(s,util,depot,"The stored fluid gives the air current its processing effect");
        s.world().modifyBlock(B,state->state.setValue(FluidAtomizerBlock.POWERED,true),false);
        text(s,util,B,"Redstone stops the spray without stopping the air current"); s.markAsFinished();
    }
    public static void valves(SceneBuilder builder,SceneBuildingUtil util) {
        var s=begin(builder,"fluid_switching","Switching fluid routes");
        show(s,util,B,CopperDynamics.THREE_WAY_VALVE.get().defaultBlockState());
        show(s,util,B.north(),AllBlocks.FLUID_PIPE.getDefaultState());
        show(s,util,B.south(),AllBlocks.FLUID_PIPE.getDefaultState());
        show(s,util,B.above(),AllBlocks.FLUID_PIPE.getDefaultState());
        s.overlay().showOutline(PonderPalette.BLUE,"common",util.select().position(B.north()),55);
        text(s,util,B,"The silver port is shared by both routes");
        text(s,util,B,"Without redstone, fluid can pass through the straight route");
        s.overlay().showOutline(PonderPalette.GREEN,"route",util.select().fromTo(B,B.south()),55);
        s.world().modifyBlock(B,state->state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED,true),false);
        text(s,util,B,"Redstone switches the common port to the side route");
        s.overlay().showOutline(PonderPalette.GREEN,"route",util.select().fromTo(B,B.above()),55);
        text(s,util,B,"Pumps still determine the direction of flow"); s.markAsFinished();
    }
    public static void collection(SceneBuilder builder,SceneBuildingUtil util) {
        var s=begin(builder,"fluid_collection","Collecting and supplying fluids");
        show(s,util,B,AllBlocks.FLUID_TANK.getDefaultState());
        var grate=B.above();
        show(s,util,grate,CopperDynamics.COPPER_COLLECTION_GRATE.get().defaultBlockState());
        show(s,util,grate.above(),Blocks.WATER.defaultBlockState());
        text(s,util,grate,"Collection Grates collect fluid touching their top face");
        s.world().setBlock(grate.above(),Blocks.AIR.defaultBlockState(),false); water(s,B,1000);
        text(s,util,B,"Collected fluid drains into a container directly below");
        s.world().setBlock(grate,CopperDynamics.SMART_COLLECTION_GRATE.get().defaultBlockState(),true);
        text(s,util,grate,"Smart Collection Grates add a fluid filter and redstone control");
        s.world().hideSection(util.select().fromTo(B,grate),Direction.UP); s.idle(20);
        s.world().setBlock(B,CopperDynamics.COPPER_WATER_SUPPLY.get().defaultBlockState(),false);
        s.world().showSection(util.select().position(B),Direction.DOWN);
        show(s,util,C,AllBlocks.FLUID_TANK.getDefaultState());
        text(s,util,B,"Conduit Boxes supply water to containers touching any of their six faces");
        water(s,C,4000);
        s.world().setBlock(B,CopperDynamics.COPPER_WATER_BASIN.get().defaultBlockState(),true);
        text(s,util,B,"Copper Water Basins provide water for containers and pipes without automatic supply"); s.markAsFinished();
    }
    public static void transfer(SceneBuilder builder,SceneBuildingUtil util) {
        var s=begin(builder,"fluid_transfer","Transferring between containers");
        var source=new BlockPos(2,1,1); var target=new BlockPos(2,1,3);
        show(s,util,source,AllBlocks.FLUID_TANK.getDefaultState()); water(s,source,3000);
        show(s,util,B,CopperDynamics.FLUID_TRANSMITTER.get().defaultBlockState().setValue(FluidTransmitterBlock.FACING,Direction.SOUTH));
        show(s,util,target,AllBlocks.FLUID_TANK.getDefaultState());
        text(s,util,B,"Fluid Transmitters move fluid between adjacent containers without rotational power");
        s.overlay().showOutline(PonderPalette.BLUE,"input",util.select().position(source),55);
        s.overlay().showOutline(PonderPalette.GREEN,"output",util.select().position(target),55);
        water(s,source,2000); water(s,target,1000);
        text(s,util,B,"The housing arrows point towards the receiving container");
        s.overlay().showControls(util.vector().centerOf(B),Pointing.DOWN,45).rightClick().withItem(com.simibubi.create.AllItems.WRENCH.asStack());
        s.world().modifyBlock(B,state->state.setValue(FluidTransmitterBlock.FACING,Direction.NORTH),false);
        text(s,util,B,"A Wrench reverses the transfer direction");
        s.world().modifyBlock(B,state->state.setValue(FluidTransmitterBlock.POWERED,true),false);
        text(s,util,B,"A redstone signal pauses transfer"); s.markAsFinished();
    }
    public static void pumping(SceneBuilder builder,SceneBuildingUtil util) {
        var s=begin(builder,"booster_pumping","Transporting fluids with Booster Pumps");
        var source=new BlockPos(2,1,0); var target=new BlockPos(2,1,4);
        show(s,util,source,AllBlocks.FLUID_TANK.getDefaultState()); water(s,source,4000);
        show(s,util,new BlockPos(2,1,1),AllBlocks.FLUID_PIPE.getDefaultState());
        show(s,util,B,CopperDynamics.BOOSTER_PUMP.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,Direction.SOUTH));
        show(s,util,new BlockPos(2,1,3),AllBlocks.FLUID_PIPE.getDefaultState());
        show(s,util,target,AllBlocks.FLUID_TANK.getDefaultState());
        s.world().setKineticSpeed(util.select().position(B),32);
        text(s,util,B,"Booster Pumps drive fluid through attached pipe networks");
        water(s,source,2000); water(s,target,2000);
        text(s,util,B,"Their direction marks indicate the flow towards the output");
        s.overlay().showControls(util.vector().centerOf(B),Pointing.DOWN,45).rightClick().withItem(com.simibubi.create.AllItems.WRENCH.asStack());
        s.world().modifyBlock(B,state->state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,Direction.NORTH),false);
        text(s,util,B,"Use a Wrench to reverse the pumping direction");
        text(s,util,B,"Greater pressure and range come with a higher Stress Impact"); s.markAsFinished();
    }
}
