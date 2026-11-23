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
        show(s,util,A,AllBlocks.FLUID_PIPE.getDefaultState());
        text(s,util,B,"The lower tank stores 4000 mB of one liquid fuel supplied by pipes or Fluid Arms");
        s.world().modifyBlockEntity(B,TankBlazeBurnerBlockEntity.class,be->be.tank.fill(new FluidStack(Fluids.LAVA,2000),FluidAction.EXECUTE));
        text(s,util,B,"Normally burns 1000 mB as bucket fuel; Liquid Fuel recipes use their own consumption and burn time");
        text(s,util,B,"Fluid Arms may select the Burner directly without a Fluid Hatch; solid fuels still work normally");s.markAsFinished();
    }
    public static void outputFiltering(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"atomizer_output_filter","Selecting Atomizer Outputs");
        show(s,util,B,CopperDynamics.FLUID_ATOMIZER.get().defaultBlockState().setValue(FluidAtomizerBlock.FACING,Direction.SOUTH));
        water(s,B,3000);s.world().setKineticSpeed(util.select().position(B),64);
        s.overlay().showFilterSlotInput(util.vector().centerOf(B.east()),Direction.EAST,60);
        s.world().setFilterData(util.select().position(B),FluidAtomizerBlockEntity.class,new ItemStack(Items.IRON_NUGGET));
        text(s,util,B,"Set a target item or an Item Filter on a side of the Atomizer");
        var depot=B.south(2);show(s,util,depot,AllBlocks.DEPOT.getDefaultState());
        s.world().createItemOnBeltLike(depot,Direction.NORTH,com.simibubi.create.AllItems.CRUSHED_IRON.asStack());
        text(s,util,depot,"Only recipes with a possible matching output are allowed; rejected inputs stay unchanged");
        text(s,util,depot,"Accepted recipes keep all byproducts and their original output chances");
        s.world().setFilterData(util.select().position(B),FluidAtomizerBlockEntity.class,ItemStack.EMPTY);
        text(s,util,B,"An empty filter allows all outputs; changing it restarts processing without switching the processing type");
        text(s,util,B,"Output filters do not restrict potion or entity effects");s.markAsFinished();
    }
    public static void mossification(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"mossification","Spreading Moss with Moss Suspension");
        var medium=dev.ccd.compat.ProcessingMedia.ALL.stream().filter(m->m.id.equals("moss_suspension")).findFirst().orElseThrow();
        show(s,util,A,Blocks.STONE.defaultBlockState());show(s,util,B,medium.block.get().defaultBlockState());
        text(s,util,B,"Source and flowing Moss Suspension mossify adjacent vanilla moss-replaceable blocks");
        s.world().setBlock(A,Blocks.MOSS_BLOCK.defaultBlockState(),true);
        show(s,util,A.above(),Blocks.MOSS_CARPET.defaultBlockState());
        text(s,util,A,"With space above, moss spreads and grows vegetation using vanilla bonemeal rules");
        show(s,util,C,Blocks.GLASS.defaultBlockState());
        text(s,util,C,"Blocks outside the moss-replaceable tag, such as Glass, remain unchanged");
        s.world().setBlock(B,CopperDynamics.FLUID_ATOMIZER.get().defaultBlockState(),true);
        text(s,util,B,"Exposed creatures gain Mossification and mossify valid blocks touched by their feet or body");
        text(s,util,B,"Contact and ordinary fans grant 4 seconds; atomized Mossification lasts 2 seconds and refreshes during exposure");s.markAsFinished();
    }
    public static void powderSnow(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"powder_snow_fluid","Handling Powder Snow as a Fluid");
        show(s,util,B,AllBlocks.FLUID_TANK.getDefaultState());
        s.overlay().showControls(util.vector().centerOf(B),Pointing.DOWN,50).rightClick().withItem(new ItemStack(Items.POWDER_SNOW_BUCKET));
        text(s,util,B,"Powder Snow Buckets provide 1000 mB of Powder Snow to fluid containers");
        s.world().modifyBlockEntity(B,net.minecraft.world.level.block.entity.BlockEntity.class,be->{var cap=be.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,B,null);if(cap!=null)cap.fill(new FluidStack(dev.ccd.CCDFluids.POWDER_SNOW.get(),1000),FluidAction.EXECUTE);});
        show(s,util,C,AllBlocks.FLUID_PIPE.getDefaultState());
        text(s,util,C,"Pipes and fluid machines can transport it like other stored fluids");
        s.world().setBlock(B,CopperDynamics.FLUID_ATOMIZER.get().defaultBlockState(),true);
        text(s,util,B,"An Atomizer can use it for Freezing when a supported processing addon supplies that type");
        text(s,util,B,"This does not automatically enable every snow-related processing type from other addons");s.markAsFinished();
    }
    public static void fluidArm(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"fluid_arm_setup","Setting Up a Mechanical Fluid Arm");
        show(s,util,A,AllBlocks.FLUID_TANK.getDefaultState());water(s,A,3000);
        show(s,util,C,AllBlocks.FLUID_TANK.getDefaultState());
        var input=A.south();var output=C.south();
        show(s,util,input,CopperDynamics.FLUID_HATCH.get().defaultBlockState().setValue(FluidHatchBlock.FACING,Direction.NORTH));
        show(s,util,output,CopperDynamics.FLUID_HATCH.get().defaultBlockState().setValue(FluidHatchBlock.FACING,Direction.NORTH));
        show(s,util,B.south(2),CopperDynamics.FLUID_ARM.get().defaultBlockState());
        s.overlay().showControls(util.vector().centerOf(A),Pointing.DOWN,50).rightClick().withItem(new ItemStack(CopperDynamics.FLUID_ARM_ITEM.get()));
        s.overlay().showOutline(PonderPalette.BLUE,"arm_input",util.select().position(A),70);
        text(s,util,A,"While holding the Fluid Arm, select accessible fluid endpoints before placing it");
        s.overlay().showOutline(PonderPalette.GREEN,"arm_output",util.select().position(C),70);
        text(s,util,output,"R-Click a Fluid Hatch to select an input; click it again to switch between input and output");
        s.world().setKineticSpeed(util.select().position(B.south(2)),32);
        water(s,A,2000);water(s,C,1000);
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
    public static void exposure(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"fluid_exposure","Processing Media and Living Creatures");
        show(s,util,B,CopperDynamics.FLUID_ATOMIZER.get().defaultBlockState());
        text(s,util,B,"Withering Fluid applies Wither; Tear Solution applies Blindness and Weakness");
        text(s,util,B,"Conduit Energy Fluid grants Water Breathing and Haste; Corrupting Fluid applies Wither and Weakness");
        text(s,util,B,"Direct contact and ordinary fans use normal effects; atomized effects are stronger and shorter");
        text(s,util,B,"Purifying Solution removes harmful effects, extinguishes fire and heals gradually");
        text(s,util,B,"Item processing needs the matching addon; Corrupting processing also requires AnvilCraft or L2 Hostility");s.markAsFinished();
    }
    public static void haunting(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"fluid_haunting","Bulk Haunting with Soul Fluid");
        show(s,util,B,CopperDynamics.HAUNTING_BLOCK.get().defaultBlockState());
        text(s,util,B,"Air currents passing through Soul Fluid process items using Haunting recipes");
        s.world().setBlock(B,CopperDynamics.FLUID_ATOMIZER.get().defaultBlockState(),true);
        text(s,util,B,"Fluid Atomizers can use Soul Fluid to provide the same Haunting effect");s.markAsFinished();
    }
    public static void gluing(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"bulk_gluing","Bulk Gluing with Slime Fluid");
        show(s,util,B,CopperDynamics.SLIME_BLOCK.get().defaultBlockState());
        text(s,util,B,"Air currents passing through Slime Fluid apply Bulk Gluing to items");
        s.world().setBlock(B,CopperDynamics.FLUID_ATOMIZER.get().defaultBlockState(),true);
        text(s,util,B,"Fluid Atomizers can use Slime Fluid to provide the same processing effect");
        text(s,util,B,"Recipes with one item and one Slimeball are read automatically, such as Pistons becoming Sticky Pistons");s.markAsFinished();
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
        text(s,util,depot,"Air currents passing through the Atomizer can process items in bulk");
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
        s.overlay().showFilterSlotInput(util.vector().centerOf(grate),Direction.SOUTH,45);
        s.world().setFilterData(util.select().position(grate),SmartCollectionGrateBlockEntity.class,new ItemStack(Items.WATER_BUCKET));
        text(s,util,grate,"Use a filled container or Fluid Filter in the filter slot; lit red indicators mean a Redstone signal is pausing the grate");
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
    public static void pumpVariants(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"pump_variants","Choosing Fluid Pump Connections");
        show(s,util,B,CopperDynamics.TURNING_BOOSTER_PUMP.get().defaultBlockState());
        text(s,util,B,"Turning Booster Pumps move fluids between two adjacent pipe mouths");
        s.world().setBlock(B,CopperDynamics.VERTICAL_TURNING_BOOSTER_PUMP.get().defaultBlockState(),true);
        text(s,util,B,"Perpendicular Turning Booster Pumps place their shaft normal to the two pipe mouths");
        s.world().setBlock(B,CopperDynamics.REDSTONE_FLUID_PUMP.get().defaultBlockState(),true);
        text(s,util,B,"A Redstone Signal reverses the Redstone Pump's fluid flow");
        s.world().modifyBlock(B,state->state.setValue(RedstoneFluidPumpBlock.FACING,Direction.UP),true);
        text(s,util,B,"Wrench a face to rotate the entire pump around that face axis; pipe ends roll the shaft without changing the pipe direction");
        s.world().setBlock(B,CopperDynamics.BIDIRECTIONAL_FLUID_PUMP.get().defaultBlockState(),true);
        text(s,util,B,"Bidirectional Pumps reverse fluid flow when their rotation reverses");
        s.world().setBlock(B,CopperDynamics.VARIABLE_FLOW_PUMP.get().defaultBlockState(),true);
        text(s,util,B,"Variable Flow Pumps use Redstone strength to adjust flow and range; Cogwheels connect above or below");
        s.world().setBlock(B,CopperDynamics.PORTABLE_PUMP.get().defaultBlockState(),true);
        s.overlay().showControls(util.vector().centerOf(B),Pointing.DOWN,45).rightClick();
        text(s,util,B,"R-Click Portable Pumps to operate them manually; Sneak to reverse manual rotation");s.markAsFinished();
    }
    public static void buffering(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"buffered_delivery","Supplying Stored Fluids");
        show(s,util,B,CopperDynamics.BUFFER_PUMP.get().defaultBlockState());
        text(s,util,B,"Buffer Pumps hold 2000 mB supplied by pipes or handheld containers");
        text(s,util,B,"Rotational power delivers stored fluid through the outlet without extracting from other containers");
        s.world().setBlock(B,CopperDynamics.CREATIVE_DELIVERY_TANK.get().defaultBlockState(),true);
        text(s,util,B,"R-Click with a filled container to select the fluid supplied by a Creative Delivery Tank");
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
        text(s,util,B,"Supply rotational power at a Shaft port to draw from inputs and deliver to outputs");
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
    public static void media(SceneBuilder builder,SceneBuildingUtil util){
        var s=begin(builder,"fluid_media","Using Processing Fluids");show(s,util,B,CopperDynamics.FLUID_ATOMIZER.get().defaultBlockState());
        text(s,util,B,"Fluid Atomizers use stored fluid to give air currents a processing effect");
        text(s,util,B,"Optional processing fluids require the matching addon to provide their fan-processing type");
        s.world().setBlock(B,AllBlocks.BLAZE_BURNER.getDefaultState(),true);
        text(s,util,B,"Super Lava can also fuel Blaze Burners, providing 160 seconds of Superheating per 1000 mB");s.markAsFinished();
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
        text(s,util,B,"Their arrow indicates the direction of flow");
        s.overlay().showControls(util.vector().centerOf(B),Pointing.DOWN,45).rightClick().withItem(com.simibubi.create.AllItems.WRENCH.asStack());
        s.world().modifyBlock(B,state->state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,Direction.NORTH),false);
        text(s,util,B,"A Wrench can be used to reverse the direction");
        text(s,util,B,"Greater pumping pressure and range come with a higher Stress Impact"); s.markAsFinished();
    }
}
