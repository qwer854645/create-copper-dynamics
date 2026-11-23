package dev.ccd.client.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
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
    public static void tankBurner(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"tank_burner","Supplying a Tank Blaze Burner");
        show(s,util,B,CopperDynamics.TANK_BURNER.get().defaultBlockState());
        show(s,util,A,AllBlocks.FLUID_PIPE.get().getAxisState(Direction.Axis.X));
        text(s,util,B,"The lower tank stores 4000 mB of one liquid fuel supplied by pipes or Fluid Arms");
        s.world().modifyBlockEntity(B,TankBlazeBurnerBlockEntity.class,be->be.tank.fill(new FluidStack(Fluids.LAVA,2000),FluidAction.EXECUTE));
        s.world().modifyBlock(B,state->state.setValue(com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL,
            com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.KINDLED),true);
        text(s,util,B,"Normally burns 1000 mB as bucket fuel; Liquid Fuel recipes use their own consumption and burn time");
        text(s,util,B,"Fluid Arms may select the Burner directly without a Fluid Hatch; solid fuels still work normally");s.markAsFinished();
    }
    public static void outputFiltering(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"atomizer_output_filter","Selecting Atomizer Outputs");
        show(s,util,B,CopperDynamics.FLUID_ATOMIZER.get().defaultBlockState().setValue(FluidAtomizerBlock.FACING,Direction.SOUTH));
        water(s,B,3000);s.world().setKineticSpeed(util.select().position(B),64);
        s.overlay().showFilterSlotInput(util.vector().of(3.01,1.5,2.25),Direction.EAST,60);
        s.world().setFilterData(util.select().position(B),FluidAtomizerBlockEntity.class,new ItemStack(Items.IRON_NUGGET));
        text(s,util,B,"Set a target item or an Item Filter on a side of the Atomizer");
        var depot=B.south(2);show(s,util,depot,AllBlocks.DEPOT.getDefaultState());
        s.world().createItemOnBeltLike(depot,Direction.NORTH,com.simibubi.create.AllItems.CRUSHED_IRON.asStack());
        text(s,util,depot,"Only recipes with a possible matching output are allowed; rejected inputs stay unchanged");
        s.world().modifyBlockEntity(depot,com.simibubi.create.content.logistics.depot.DepotBlockEntity.class,be->be.getBehaviour(com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TYPE)
            .handleProcessingOnAllItems(item->{var result=item.copy();result.stack=new ItemStack(Items.IRON_NUGGET,9);return com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult.convertToAndLeaveHeld(java.util.List.of(result),null);}));
        text(s,util,depot,"Accepted recipes keep all byproducts and their original output chances");
        s.world().setFilterData(util.select().position(B),FluidAtomizerBlockEntity.class,new ItemStack(Items.DIAMOND));
        s.world().createItemOnBeltLike(depot,Direction.NORTH,com.simibubi.create.AllItems.CRUSHED_IRON.asStack());
        s.overlay().showOutline(PonderPalette.RED,"rejected_input",util.select().position(depot),60);
        s.idle(40);
        s.world().setFilterData(util.select().position(B),FluidAtomizerBlockEntity.class,ItemStack.EMPTY);
        text(s,util,B,"An empty filter allows all outputs; changing it restarts processing without switching the processing type");
        text(s,util,B,"Output filters do not restrict potion or entity effects");s.markAsFinished();
    }
    public static void fluidArm(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"fluid_arm_setup","Setting Up a Mechanical Fluid Arm");
        show(s,util,A,AllBlocks.FLUID_TANK.getDefaultState());water(s,A,3000);
        show(s,util,C,AllBlocks.FLUID_TANK.getDefaultState());
        var input=A.south();var output=C.south();
        show(s,util,input,CopperDynamics.FLUID_HATCH.get().defaultBlockState().setValue(FluidHatchBlock.FACING,Direction.NORTH));
        show(s,util,output,CopperDynamics.FLUID_HATCH.get().defaultBlockState().setValue(FluidHatchBlock.FACING,Direction.NORTH));
        show(s,util,B.south(2),CopperDynamics.FLUID_ARM.get().defaultBlockState());
        s.overlay().showControls(util.vector().blockSurface(input,Direction.SOUTH),Pointing.DOWN,50).rightClick().withItem(new ItemStack(CopperDynamics.FLUID_ARM_ITEM.get()));
        s.overlay().showOutline(PonderPalette.BLUE,"arm_input",util.select().position(input),70);
        text(s,util,input,"While holding the Fluid Arm, select accessible fluid endpoints before placing it");
        s.overlay().showOutline(PonderPalette.GREEN,"arm_output",util.select().position(C),70);
        text(s,util,output,"R-Click a Fluid Hatch to select an input; click it again to switch between input and output");
        s.world().setKineticSpeed(util.select().position(B.south(2)),32);
        s.world().modifyBlockEntity(B.south(2),FluidArmBlockEntity.class,be->{
            var world=be.getLevel();
            be.configure(java.util.List.of(
                new dev.ccd.logistics.FluidArmPoint(world,input,world.getBlockState(input),Direction.SOUTH,com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint.Mode.TAKE),
                new dev.ccd.logistics.FluidArmPoint(world,output,world.getBlockState(output),Direction.SOUTH,com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint.Mode.DEPOSIT)));
            ((dev.ccd.mixin.FluidArmAccessor)(Object)be).ccd$held(FluidArmCargoItem.containing(new FluidStack(Fluids.WATER,1000)));
        });
        water(s,A,2000);s.idle(30);
        s.world().modifyBlockEntity(B.south(2),FluidArmBlockEntity.class,be->((dev.ccd.mixin.FluidArmAccessor)(Object)be).ccd$held(ItemStack.EMPTY));
        water(s,C,1000);
        text(s,util,B.south(2),"Place the configured Arm and supply rotational power to transfer fluid between the selected locations");
        text(s,util,C,"Fluid Hatches can provide controlled access to the containers behind them");s.markAsFinished();
    }
    public static void repackaging(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"fluid_repackaging","Repacking Fluid Orders");
        show(s,util,B,CopperDynamics.FLUID_REPACKAGER.get().defaultBlockState().setValue(PackagerBlock.FACING,Direction.SOUTH));
        var inventory=B.north();show(s,util,inventory,Blocks.BARREL.defaultBlockState());
        var items=new net.neoforged.neoforge.items.ItemStackHandler(9);
        items.setStackInSlot(0,new ItemStack(Items.IRON_INGOT,4));
        var itemBox=com.simibubi.create.content.logistics.box.PackageItem.containing(items);
        var fluidBox=FluidPackageItem.containingFluid(new FluidStack(Fluids.WATER,1000));
        com.simibubi.create.content.logistics.box.PackageItem.setOrder(itemBox,42,0,true,0,false,null);
        com.simibubi.create.content.logistics.box.PackageItem.setOrder(fluidBox,42,0,true,1,true,null);
        for(var box:java.util.List.of(itemBox,fluidBox))com.simibubi.create.content.logistics.box.PackageItem.addAddress(box,"Workshop");
        s.world().modifyBlockEntity(inventory,net.minecraft.world.level.block.entity.BarrelBlockEntity.class,be->{be.setItem(0,itemBox.copy());be.setItem(1,fluidBox.copy());});
        var depot=B.south();show(s,util,depot,AllBlocks.DEPOT.getDefaultState());
        s.world().createItemOnBeltLike(depot,Direction.NORTH,FluidPackageItem.containingFluid(new FluidStack(Fluids.WATER,1000)));
        text(s,util,depot,"Fluid orders travel in Copper Packages; item packages can travel alongside them");
        var output=CompositePackageItem.containing(items,java.util.List.of(new FluidStack(Fluids.WATER,1000)));
        com.simibubi.create.content.logistics.box.PackageItem.addAddress(output,"Workshop");
        s.world().modifyBlockEntity(inventory,net.minecraft.world.level.block.entity.BarrelBlockEntity.class,be->be.clearContent());
        s.world().modifyBlockEntity(B,PackagerBlockEntity.class,be->{be.heldBox=output.copy();be.animationInward=false;be.animationTicks=PackagerBlockEntity.CYCLE;});
        text(s,util,B,"The Fluid Repackager combines items and fluids belonging to the same order into Brass Packages");
        text(s,util,B,"Preserve the logistics order and address information when routing packages to the Repackager");
        text(s,util,B,"Collect all packages of the same order in the item container behind the Repackager");s.markAsFinished();
    }
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
        int duration=Math.max(60,Math.min(140,message.length()));
        scene.addKeyframe();
        scene.overlay().showText(duration).text(message).pointAt(util.vector().centerOf(pos)).placeNearTarget(); scene.idle(duration+10);
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
        text(s,util,A,"Attach fluid packagers to the fluid container they should target");
        s.overlay().showOutline(PonderPalette.BLUE,"source",util.select().position(A),45);
        text(s,util,B,"Given redstone power, it will pack fluids from the container into a package");
        s.world().modifyBlockEntity(B,FluidPackagerBlockEntity.class,be->{be.heldBox=FluidPackageItem.containingFluid(new FluidStack(Fluids.WATER,4000));}); water(s,A,0);
        text(s,util,B,"Copper Packages can travel on Belts and package logistics");
        s.world().modifyBlockEntity(B,FluidPackagerBlockEntity.class,be->{be.heldBox=ItemStack.EMPTY;}); water(s,A,4000);
        text(s,util,A,"Packages inserted will be destroyed, unpacking the fluids into the container");
        s.overlay().showControls(util.vector().centerOf(B),Pointing.DOWN,45).rightClick().withItem(new ItemStack(Items.WATER_BUCKET));
        text(s,util,B,"R-Click with a filled container to filter Redstone-triggered packaging");
        s.world().setBlock(A,net.minecraft.world.level.block.Blocks.BARREL.defaultBlockState(),true);
        var contents=new net.neoforged.neoforge.items.ItemStackHandler(9);
        contents.setStackInSlot(0,new ItemStack(Items.IRON_INGOT,4));
        var brass=CompositePackageItem.containing(contents,java.util.List.of(new FluidStack(Fluids.WATER,1000)));
        s.world().modifyBlockEntity(B,FluidPackagerBlockEntity.class,be->{be.previouslyUnwrapped=brass.copy();be.animationInward=true;be.animationTicks=PackagerBlockEntity.CYCLE;});
        s.idle(20);
        s.world().modifyBlockEntity(A,net.minecraft.world.level.block.entity.BarrelBlockEntity.class,be->be.setItem(0,new ItemStack(Items.IRON_INGOT,4)));
        s.world().modifyBlockEntity(B,FluidPackagerBlockEntity.class,be->{be.heldBox=FluidPackageItem.containingFluid(new FluidStack(Fluids.WATER,1000));be.animationInward=false;be.animationTicks=PackagerBlockEntity.CYCLE;});
        text(s,util,B,"Brass Packages deposit their items into the rear item container and output their fluids in Copper Packages"); s.markAsFinished();
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
        text(s,util,B,"Smart Fluid Hatches can restrict which fluids may pass");
        s.world().setFilterData(util.select().position(B),SmartFluidHatchBlockEntity.class,new ItemStack(Items.WATER_BUCKET));
        s.markAsFinished();
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
        text(s,util,B,"Use filters to assign different fluids to each container"); s.markAsFinished();
    }
    public static void tanks(SceneBuilder builder,SceneBuildingUtil util) {
        var s=begin(builder,"smart_tanks","Dimensions of a Smart Fluid Tank");
        var origin=new BlockPos(1,1,1);
        tankStructure(s,util,origin,false);
        text(s,util,B,"Smart Fluid Tanks can be combined to increase the total capacity");
        text(s,util,B.above(),"Their cross-section can be 1×1, 2×2 or 3×3 blocks");
        s.world().hideSection(util.select().fromTo(origin,origin.offset(1,2,1)),Direction.UP); s.idle(20);
        for(var p:BlockPos.betweenClosed(origin,origin.offset(1,2,1)))
            s.world().setBlock(p.immutable(),Blocks.AIR.defaultBlockState(),false);
        tankStructure(s,util,origin,true);
        text(s,util,B,"Horizontal Smart Fluid Tanks can be extended horizontally");
        s.world().modifyBlockEntity(origin,SmartFluidTankBlockEntity.class,tank->{
            tank.inventory.fill(new FluidStack(Fluids.WATER,24000),FluidAction.EXECUTE);
            tank.inventory.fill(new FluidStack(Fluids.LAVA,16000),FluidAction.EXECUTE);
        });
        text(s,util,A,"A single tank structure can store multiple fluids");
        s.overlay().showControls(util.vector().centerOf(B),Pointing.DOWN,45).rightClick().withItem(com.simibubi.create.AllItems.WRENCH.asStack());
        text(s,util,B,"Using a Wrench, a tanks' window can be toggled"); s.markAsFinished();
    }
    /** Ponder does not run server connectivity: stage complete controller/member data explicitly. */
    private static void tankStructure(CreateSceneBuilder s,SceneBuildingUtil util,BlockPos origin,boolean horizontal) {
        var block=horizontal?CopperDynamics.HORIZONTAL_SMART_TANK.get():CopperDynamics.SMART_FLUID_TANK.get();
        for(int layer=0;layer<3;layer++) {
            for(int x=0;x<2;x++) for(int z=0;z<2;z++) {
                var pos=origin.offset(horizontal?layer:x,horizontal?x:layer,z);
                int localX=horizontal?z:x,localZ=horizontal?1-x:z;
                var shape=localX==0
                    ?(localZ==0?com.simibubi.create.content.fluids.tank.FluidTankBlock.Shape.WINDOW_NW:com.simibubi.create.content.fluids.tank.FluidTankBlock.Shape.WINDOW_SW)
                    :(localZ==0?com.simibubi.create.content.fluids.tank.FluidTankBlock.Shape.WINDOW_NE:com.simibubi.create.content.fluids.tank.FluidTankBlock.Shape.WINDOW_SE);
                var state=block.defaultBlockState()
                    .setValue(com.simibubi.create.content.fluids.tank.FluidTankBlock.BOTTOM,layer==0)
                    .setValue(com.simibubi.create.content.fluids.tank.FluidTankBlock.TOP,layer==2)
                    .setValue(com.simibubi.create.content.fluids.tank.FluidTankBlock.SHAPE,shape);
                if(horizontal) state=state.setValue(HorizontalSmartTankBlock.AXIS,Direction.Axis.X);
                s.world().setBlock(pos,state,false);
                s.world().modifyBlockEntity(pos,SmartFluidTankBlockEntity.class,tank->{
                    tank.preventConnectivityUpdate();
                    tank.setController(origin);
                    tank.setWidth(2); tank.setHeight(3); tank.setWindows(true);
                });
            }
            var from=origin.offset(horizontal?layer:0,horizontal?0:layer,0);
            var to=from.offset(horizontal?0:1,horizontal?1:0,1);
            s.world().showSection(util.select().fromTo(from,to),Direction.DOWN); s.idle(15);
        }
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
        s.world().modifyBlock(faucet,state->state.setValue(FaucetBlock.OPEN,true),false);
        s.world().setBlock(target,Blocks.AIR.defaultBlockState(),false);
        show(s,util,faucet.below(2),AllBlocks.DEPOT.getDefaultState());
        s.world().createItemOnBeltLike(faucet.below(2),Direction.NORTH,new ItemStack(Items.GLASS_BOTTLE));
        s.idle(25);water(s,source,3750);
        s.world().modifyBlockEntity(faucet.below(2),com.simibubi.create.content.logistics.depot.DepotBlockEntity.class,be->be.getBehaviour(com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TYPE)
            .handleProcessingOnAllItems(item->{var result=item.copy();result.stack=net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION,net.minecraft.world.item.alchemy.Potions.WATER);return com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult.convertToAndLeaveHeld(java.util.List.of(result),null);}));
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
        text(s,util,depot,"Air currents passing through the Atomizer can process items in bulk");
        s.world().modifyBlockEntity(depot,com.simibubi.create.content.logistics.depot.DepotBlockEntity.class,be->be.getBehaviour(com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TYPE)
            .handleProcessingOnAllItems(item->{var result=item.copy();result.stack=new ItemStack(Items.IRON_NUGGET,9);return com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult.convertToAndLeaveHeld(java.util.List.of(result),null);}));
        s.idle(25);
        s.world().modifyBlock(B,state->state.setValue(FluidAtomizerBlock.POWERED,true),false);
        text(s,util,B,"A Redstone Signal stops atomizing without stopping the air current"); s.markAsFinished();
    }
    public static void valves(SceneBuilder builder,SceneBuildingUtil util) {
        var s=begin(builder,"fluid_switching","Switching Fluid Pipe Connections");
        show(s,util,B,CopperDynamics.THREE_WAY_VALVE.get().defaultBlockState());
        show(s,util,B.north(),AllBlocks.FLUID_PIPE.get().getAxisState(Direction.Axis.Z));
        show(s,util,B.south(),AllBlocks.FLUID_PIPE.get().getAxisState(Direction.Axis.Z));
        show(s,util,B.above(),AllBlocks.FLUID_PIPE.get().getAxisState(Direction.Axis.Y));
        s.overlay().showOutline(PonderPalette.BLUE,"common",util.select().position(B.north()),55);
        text(s,util,B,"The silver connector is shared by both connections");
        text(s,util,B,"Without a Redstone Signal, it connects to the opposite end");
        s.overlay().showOutline(PonderPalette.GREEN,"route",util.select().fromTo(B,B.south()),55);
        s.world().modifyBlock(B,state->state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED,true),false);
        show(s,util,B.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());
        text(s,util,B,"A Redstone Signal switches it to the side connection");
        s.overlay().showOutline(PonderPalette.GREEN,"route",util.select().fromTo(B,B.above()),55);
        text(s,util,B,"Mechanical Pumps still determine the direction of flow"); s.markAsFinished();
    }
    public static void collection(SceneBuilder builder,SceneBuildingUtil util) {
        var s=begin(builder,"fluid_collection","Collecting Fluids");
        show(s,util,B,AllBlocks.FLUID_TANK.getDefaultState());
        var grate=B.above();
        show(s,util,grate,CopperDynamics.COPPER_COLLECTION_GRATE.get().defaultBlockState());
        show(s,util,grate.above(),Blocks.WATER.defaultBlockState());
        text(s,util,grate,"Collection Grates collect fluids touching their top face");
        s.world().setBlock(grate.above(),Blocks.AIR.defaultBlockState(),false); water(s,B,1000);
        text(s,util,B,"Collected fluids drain into a container directly below");
        s.world().setBlock(grate,CopperDynamics.SMART_COLLECTION_GRATE.get().defaultBlockState(),true);
        text(s,util,grate,"Smart Collection Grates can filter fluids; Redstone pauses collection and draining");
        s.overlay().showFilterSlotInput(util.vector().centerOf(grate),Direction.SOUTH,45);
        s.world().setFilterData(util.select().position(grate),SmartCollectionGrateBlockEntity.class,new ItemStack(Items.WATER_BUCKET));
        text(s,util,grate,"Use a filled container or Fluid Filter in the filter slot; lit red indicators mean a Redstone signal is pausing the grate");
        s.world().modifyBlock(grate,state->state.setValue(SmartCollectionGrateBlock.POWERED,true),true);
        show(s,util,grate.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());
        s.idle(25);s.markAsFinished();
    }
    public static void waterSupply(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"water_supply","Supplying Adjacent Containers");
        show(s,util,B,CopperDynamics.COPPER_WATER_SUPPLY.get().defaultBlockState());
        show(s,util,C,AllBlocks.FLUID_TANK.getDefaultState());
        text(s,util,B,"Water Supply Boxes automatically supply water to adjacent containers on all six sides");
        water(s,C,4000);
        s.world().modifyBlock(B,state->state.setValue(CopperWaterSupplyBlock.POWERED,true),true);
        s.markAsFinished();
    }
    public static void waterBasin(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"water_basin","Drawing Unlimited Water");
        show(s,util,B,CopperDynamics.COPPER_WATER_BASIN.get().defaultBlockState());
        s.overlay().showControls(util.vector().topOf(B),Pointing.DOWN,45).rightClick().withItem(new ItemStack(Items.BUCKET));
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
    public static void ordering(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"fluid_ordering","Monitoring and Ordering Fluids");
        var tank=new BlockPos(2,1,1);var packager=new BlockPos(2,1,2);
        var gauge=packager.south();var link=packager.above();
        var network=java.util.UUID.fromString("ccd00000-0000-0000-0000-000000000001");
        show(s,util,tank,AllBlocks.FLUID_TANK.getDefaultState());water(s,tank,1000);
        show(s,util,packager,CopperDynamics.FLUID_PACKAGER.get().defaultBlockState().setValue(PackagerBlock.FACING,Direction.SOUTH));
        show(s,util,link,AllBlocks.STOCK_LINK.getDefaultState()
            .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.ATTACH_FACE,
                net.minecraft.world.level.block.state.properties.AttachFace.FLOOR));
        s.overlay().showControls(util.vector().topOf(link),Pointing.DOWN,50).rightClick()
            .withItem(new ItemStack(CopperDynamics.FLUID_GAUGE_ITEM.get()));
        text(s,util,link,"Use a Fluid Factory Gauge on a Stock Link to select its logistics network");
        show(s,util,gauge,CopperDynamics.FLUID_GAUGE.get().defaultBlockState()
            .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.ATTACH_FACE,
                net.minecraft.world.level.block.state.properties.AttachFace.WALL)
            .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
        s.world().modifyBlockEntity(gauge,FluidGaugeBlockEntity.class,be->{
            be.addPanel(com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot.TOP_LEFT,network);
            be.restocker=true;
        });
        text(s,util,gauge,"Place the Gauge on a receiving Fluid Packager to maintain stock in its rear container");
        s.overlay().showControls(util.vector().blockSurface(gauge,Direction.SOUTH),Pointing.RIGHT,50)
            .rightClick().withItem(new ItemStack(Items.WATER_BUCKET));
        s.world().modifyBlockEntity(gauge,FluidGaugeBlockEntity.class,be->{
            var panel=be.panels.get(com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot.TOP_LEFT);
            panel.setFilter(new ItemStack(Items.WATER_BUCKET));panel.count=4000;panel.upTo=true;
        });
        text(s,util,gauge,"Select a fluid with a filled container and set the target amount in mB");
        s.world().modifyBlockEntity(packager,FluidPackagerBlockEntity.class,be->{
            be.previouslyUnwrapped=FluidPackageItem.containingFluid(new FluidStack(Fluids.WATER,3000));
            be.animationInward=true;be.animationTicks=PackagerBlockEntity.CYCLE;
        });
        water(s,tank,4000);
        s.world().modifyBlockEntity(gauge,FluidGaugeBlockEntity.class,be->{
            be.panels.get(com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot.TOP_LEFT).satisfied=true;
        });
        text(s,util,tank,"Deliveries arrive as Copper Packages and replenish the selected fluid");
        s.world().modifyBlockEntity(gauge,FluidGaugeBlockEntity.class,be->{
            be.panels.get(com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot.TOP_LEFT).count=0;
        });
        text(s,util,gauge,"Set the target to zero to monitor stock without requesting deliveries");
        s.markAsFinished();
    }
    public static void turningPump(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"turning_pump","Turning Fluid Flow");
        var state=CopperDynamics.TURNING_BOOSTER_PUMP.get().defaultBlockState().setValue(TurningBoosterPumpBlock.FACING,Direction.SOUTH);
        show(s,util,B,state);show(s,util,B.south(),AllBlocks.FLUID_PIPE.get().getAxisState(Direction.Axis.Z));
        var branch=TurningBoosterPumpBlock.branch(state);show(s,util,B.relative(branch),AllBlocks.FLUID_PIPE.get().getAxisState(branch.getAxis()));
        s.world().setKineticSpeed(util.select().position(B),32);
        text(s,util,B,"Transfers fluid between adjacent mouths; the shafts lie in the same plane as the mouths");s.markAsFinished();
    }
    public static void verticalTurningPump(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"vertical_turning_pump","Connecting Perpendicular Shafts");
        show(s,util,B,CopperDynamics.VERTICAL_TURNING_BOOSTER_PUMP.get().defaultBlockState());
        s.world().setKineticSpeed(util.select().position(B),32);
        text(s,util,B,"The through-shaft is perpendicular to the plane of the two adjacent pipe mouths");s.markAsFinished();
    }
    public static void redstonePump(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"redstone_pump","Reversing Flow with Redstone");
        show(s,util,B,CopperDynamics.REDSTONE_FLUID_PUMP.get().defaultBlockState().setValue(RedstoneFluidPumpBlock.FACING,Direction.SOUTH));
        s.world().setKineticSpeed(util.select().position(B),32);
        text(s,util,B,"Supply rotational power to pump fluid");
        show(s,util,B.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());
        s.world().modifyBlock(B,state->state.setValue(RedstoneFluidPumpBlock.POWERED,true),true);
        text(s,util,B,"A Redstone Signal reverses flow without reversing the drive");s.markAsFinished();
    }
    public static void bidirectionalPump(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"bidirectional_pump","Reversing Flow with Rotation");
        show(s,util,B,CopperDynamics.BIDIRECTIONAL_FLUID_PUMP.get().defaultBlockState());
        s.world().setKineticSpeed(util.select().position(B),32);
        text(s,util,B,"The drive direction determines the fluid direction");
        s.world().setKineticSpeed(util.select().position(B),-32);
        s.world().modifyBlock(B,state->state.setValue(BidirectionalFluidPumpBlock.REVERSED,true),true);
        text(s,util,B,"Reverse the rotation to reverse fluid flow");s.markAsFinished();
    }
    public static void variablePump(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"variable_pump","Controlling Pump Flow");
        show(s,util,B,CopperDynamics.VARIABLE_FLOW_PUMP.get().defaultBlockState().setValue(VariableFlowPumpBlock.FACING,Direction.SOUTH));
        s.world().setKineticSpeed(util.select().position(B),32);
        text(s,util,B,"Supply rotation through the exposed Cogwheel faces");
        show(s,util,B.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());
        s.world().modifyBlock(B,state->state.setValue(VariableFlowPumpBlock.SIGNAL,15),true);
        text(s,util,B,"Redstone strength controls flow and range; the dial shows signal strength");
        s.world().setBlock(B.east(),Blocks.AIR.defaultBlockState(),true);
        s.world().modifyBlock(B,state->state.setValue(VariableFlowPumpBlock.SIGNAL,0),true);
        text(s,util,B,"With the default settings, no signal stops pumping while the gear can still turn");s.markAsFinished();
    }
    public static void portablePump(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"portable_pump","Operating a Portable Pump");
        show(s,util,B,CopperDynamics.PORTABLE_PUMP.get().defaultBlockState());
        s.overlay().showControls(util.vector().topOf(B),Pointing.DOWN,45).rightClick();
        s.world().modifyBlockEntity(B,PortableFluidPumpBlockEntity.class,be->be.setSpeed(32));
        text(s,util,B,"R-Click to pump manually; Sneak-R-Click to reverse manual rotation");
        s.world().setKineticSpeed(util.select().position(B),-32);
        text(s,util,B,"A shaft can also drive the pump continuously");s.markAsFinished();
    }
    public static void buffering(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"buffered_delivery","Supplying Stored Fluids");
        show(s,util,B,CopperDynamics.BUFFER_PUMP.get().defaultBlockState().setValue(BufferPumpBlock.FACING,Direction.SOUTH));
        text(s,util,B,"Buffer Pumps hold 2000 mB supplied by pipes or handheld containers");
        text(s,util,B,"Rotational power delivers stored fluid through the outlet without extracting from other containers");
        show(s,util,B.south(),AllBlocks.FLUID_TANK.getDefaultState());
        s.world().modifyBlockEntity(B,BufferPumpBlockEntity.class,be->be.buffer.fill(new FluidStack(Fluids.WATER,2000),FluidAction.EXECUTE));
        s.world().setKineticSpeed(util.select().position(B),32);s.idle(25);
        s.world().modifyBlockEntity(B,BufferPumpBlockEntity.class,be->be.buffer.drain(1000,FluidAction.EXECUTE));water(s,B.south(),1000);
        s.markAsFinished();
    }
    public static void creativeSupply(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"creative_supply","Supplying Unlimited Fluid");
        show(s,util,B,CopperDynamics.CREATIVE_DELIVERY_TANK.get().defaultBlockState().setValue(CreativeDeliveryTankBlock.FACING,Direction.SOUTH));
        s.overlay().showControls(util.vector().centerOf(B),Pointing.DOWN,45).rightClick().withItem(new ItemStack(Items.WATER_BUCKET));
        s.world().modifyBlockEntity(B,CreativeDeliveryTankBlockEntity.class,be->be.select(new FluidStack(Fluids.WATER,1000)));
        show(s,util,B.south(),AllBlocks.FLUID_TANK.getDefaultState());water(s,B.south(),4000);
        text(s,util,B,"R-Click with a filled container to select the fluid supplied by a Creative Delivery Tank");
        s.world().modifyBlockEntity(B,CreativeDeliveryTankBlockEntity.class,be->be.worldOutput.setValue(1));
        text(s,util,B,"Use a Wrench to rotate the outlet; upper side controls switch between container delivery and world output");s.markAsFinished();
    }
    public static void omniPumping(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"omni_pumping","Configuring Pump Faces");
        show(s,util,B,CopperDynamics.OMNI_PUMP.get().defaultBlockState());
        text(s,util,B,"All six faces of an Omnidirectional Pump start closed");
        s.overlay().showControls(util.vector().centerOf(B),Pointing.DOWN,45).rightClick().withItem(com.simibubi.create.AllItems.WRENCH.asStack());
        s.world().modifyBlock(B,state->state.setValue(OmniPumpBlock.PORTS.get(Direction.NORTH),OmniPumpBlock.INPUT),false);
        text(s,util,B,"R-Click a face with a Wrench to cycle Input, Output, Shaft and Closed");
        s.world().modifyBlock(B,state->state.setValue(OmniPumpBlock.PORTS.get(Direction.SOUTH),OmniPumpBlock.OUTPUT).setValue(OmniPumpBlock.PORTS.get(Direction.UP),OmniPumpBlock.SHAFT),false);
        show(s,util,B.north(),AllBlocks.FLUID_TANK.getDefaultState());water(s,B.north(),4000);
        show(s,util,B.south(),AllBlocks.FLUID_TANK.getDefaultState());
        s.world().setKineticSpeed(util.select().position(B),32);
        text(s,util,B,"Supply rotational power at a Shaft port to draw from inputs and deliver to outputs");
        water(s,B.north(),3000);water(s,B.south(),1000);
        text(s,util,B,"The pipe network distributes fluid without storing it in the pump; unavailable outputs stop extraction, and rotation reversal does not swap port modes");s.markAsFinished();
    }
    public static void metering(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"flow_metering","Measuring Fluid Flow");show(s,util,B,CopperDynamics.FLOW_METER.get().getAxisState(Direction.Axis.Z));
        for(var pos:java.util.List.of(B.north(),B.south()))show(s,util,pos,AllBlocks.FLUID_PIPE.get().getAxisState(Direction.Axis.Z));
        text(s,util,B,"Install a Flow Meter between two Fluid Pipes");
        s.world().modifyBlockEntity(B,FlowMeterBlockEntity.class,be->be.flowRate=500);
        text(s,util,B,"The needle responds to transferred fluid and returns to zero when flow stops");
        s.idle(20);s.world().modifyBlockEntity(B,FlowMeterBlockEntity.class,be->be.flowRate=0);
        text(s,util,B,"Engineer’s Goggles display the current flow in mB per tick");
        s.overlay().showControls(util.vector().blockSurface(B,Direction.EAST),Pointing.DOWN,45).rightClick().withItem(com.simibubi.create.AllItems.WRENCH.asStack());
        s.world().modifyBlock(B,state->state.setValue(FlowMeterBlock.FACING,Direction.WEST),true);
        s.rotateCameraY(180);
        text(s,util,B,"Use a Wrench to change the dial orientation");s.markAsFinished();
    }
    public static void redstoneValve(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"redstone_valving","Controlling Fluid Connections");show(s,util,B,CopperDynamics.REDSTONE_FLUID_VALVE.get().defaultBlockState());
        for(var pos:java.util.List.of(B.north(),B.south()))show(s,util,pos,AllBlocks.FLUID_PIPE.get().getAxisState(Direction.Axis.Z));
        text(s,util,B,"Redstone Fluid Valves allow flow while unpowered");
        show(s,util,B.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());
        s.world().modifyBlock(B,state->state.setValue(RedstoneFluidValveBlock.POWERED,true).setValue(RedstoneFluidValveBlock.ENABLED,false),true);
        text(s,util,B,"A Redstone Signal closes the connection; removing the signal opens it again");
        s.world().setBlock(B.east(),Blocks.AIR.defaultBlockState(),true);
        s.world().modifyBlock(B,state->state.setValue(RedstoneFluidValveBlock.POWERED,false).setValue(RedstoneFluidValveBlock.ENABLED,true),true);
        text(s,util,B,"A Mechanical Pump is still required to move fluid through the pipe");s.markAsFinished();
    }
    public static void filtering(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"fluid_filtering","Filtering Fluids");show(s,util,B,CopperDynamics.SMART_FLUID_TANK.get().defaultBlockState());
        s.overlay().showControls(util.vector().centerOf(B),Pointing.DOWN,45).rightClick().withItem(new ItemStack(CopperDynamics.FLUID_FILTER.get()));
        text(s,util,B,"R-Click a Fluid Filter in hand to configure its fluid samples");
        text(s,util,B,"Copy fluids from handheld containers or drag fluid entries from a recipe browser; samples are not consumed");
        show(s,util,C,CopperDynamics.SMART_FLUID_HATCH.get().defaultBlockState().setValue(FluidHatchBlock.FACING,Direction.WEST));
        s.world().setFilterData(util.select().position(C),SmartFluidHatchBlockEntity.class,new ItemStack(Items.WATER_BUCKET));
        s.overlay().showOutline(PonderPalette.GREEN,"filter_example",util.select().position(C),60);
        text(s,util,B,"Fluid Attribute Filters use a reference fluid to select properties; Add or Add Inverted saves the selected rule");
        text(s,util,B,"Choose Any, All or Deny to combine attributes, then place the filter in a component's filter slot");s.markAsFinished();
    }
    public static void buckets(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"metal_buckets","Using High-Capacity Buckets");show(s,util,B,AllBlocks.FLUID_TANK.getDefaultState());water(s,B,4000);
        s.overlay().showControls(util.vector().centerOf(B),Pointing.DOWN,45).rightClick().withItem(new ItemStack(CopperDynamics.COPPER_BUCKET.get()));
        text(s,util,B,"Copper and Brass Buckets can hold several buckets of a single fluid");
        text(s,util,B,"R-Click containers to draw fluid; Sneak and R-Click to pour");
        text(s,util,B,"World interactions transfer one bucket at a time; Brass Buckets hold more than Copper Buckets");s.markAsFinished();
    }
    public static void pumping(SceneBuilder builder,SceneBuildingUtil util) {
        var s=begin(builder,"booster_pumping","Fluid Transportation using Booster Pumps");
        var source=new BlockPos(2,1,0); var target=new BlockPos(2,1,4);
        show(s,util,source,AllBlocks.FLUID_TANK.getDefaultState()); water(s,source,4000);
        show(s,util,new BlockPos(2,1,1),AllBlocks.FLUID_PIPE.get().getAxisState(Direction.Axis.Z));
        show(s,util,B,CopperDynamics.BOOSTER_PUMP.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,Direction.SOUTH));
        show(s,util,new BlockPos(2,1,3),AllBlocks.FLUID_PIPE.get().getAxisState(Direction.Axis.Z));
        show(s,util,target,AllBlocks.FLUID_TANK.getDefaultState());
        s.world().setKineticSpeed(util.select().position(B),32);
        text(s,util,B,"Booster Pumps govern the flow of their attached pipe networks");
        water(s,source,2000); water(s,target,2000);
        text(s,util,B,"Their arrow indicates the direction of flow");
        s.overlay().showControls(util.vector().centerOf(B),Pointing.DOWN,45).rightClick().withItem(com.simibubi.create.AllItems.WRENCH.asStack());
        s.world().modifyBlock(B,state->state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,Direction.NORTH),false);
        text(s,util,B,"A Wrench can be used to reverse the direction");
        text(s,util,B,"Greater pumping pressure and range come with a higher Stress Impact"); s.markAsFinished();
    }
}
