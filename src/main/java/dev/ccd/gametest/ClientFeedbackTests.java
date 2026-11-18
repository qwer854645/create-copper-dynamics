package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.content.*;
import com.simibubi.create.content.logistics.box.PackageEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CopperDynamics.ID)
@PrefixGameTestTemplate(false)
public final class ClientFeedbackTests {
    @GameTest(template="empty")
    public static void brassBoxOpeningPreservesReturnedHand(GameTestHelper h) {
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
        player.getInventory().clearContent();
        var items=new net.neoforged.neoforge.items.ItemStackHandler(9);
        items.setStackInSlot(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,17));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,CompositePackageItem.containing(items,java.util.List.of(new FluidStack(Fluids.WATER,5000))));
        var result=CopperDynamics.COMPOSITE_PACKAGE.get().open(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
        // Mirror ServerPlayerGameMode's write-back after Item.use returns.
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,result.getObject());
        int diamonds=0,water=0;
        for(var item:player.getInventory().items) {
            if(item.is(net.minecraft.world.item.Items.DIAMOND)) diamonds+=item.getCount();
            if(FluidPackageItem.isFluidPackage(item)) water+=FluidPackageItem.contents(item).getAmount();
        }
        h.assertTrue(diamonds==17 && water==5000,"Opening or result write-back swallowed contents");
        player.getInventory().clearContent();
        h.succeed();
    }
    @GameTest(template="empty")
    public static void sealedPackagesBreakOnceWithoutReappearing(GameTestHelper h) {
        var pos=net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(1,3,1)));
        var stack=FluidPackageItem.containingFluid(new FluidStack(Fluids.WATER,1000));
        var entity=PackageEntity.fromItemStack(h.getLevel(),pos,stack);
        h.getLevel().addFreshEntity(entity);
        entity.hurt(h.getLevel().damageSources().generic(),1);
        h.assertTrue(entity.isRemoved(),"Canister resisted normal package damage");
        h.runAfterDelay(3,()->{
            var bounds=new net.minecraft.world.phys.AABB(pos,pos).inflate(2);
            h.assertTrue(h.getLevel().getEntitiesOfClass(PackageEntity.class,bounds,e->FluidPackageItem.isFluidPackage(e.box)).isEmpty(),"Broken canister respawned as package");
            var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,bounds,e->FluidPackageItem.isFluidPackage(e.getItem()));
            h.assertTrue(drops.size()==1 && FluidPackageItem.contents(drops.getFirst().getItem()).getAmount()==1000,"Breaking duplicated or lost sealed fluid");
            h.succeed();
        });
    }
    @GameTest(template="empty")
    public static void creativeCopperBucketWorldPlacementPreservesContents(GameTestHelper h) {
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        var base=new BlockPos(1,1,1);
        h.setBlock(base,net.minecraft.world.level.block.Blocks.STONE);
        var absolute=h.absolutePos(base);
        player.setPos(absolute.getX()+.5,absolute.getY()+2,absolute.getZ()+.5);
        player.setXRot(90); player.setYRot(0);
        var stack=new net.minecraft.world.item.ItemStack(CopperDynamics.COPPER_BUCKET.get());
        stack.set(CopperDynamics.COPPER_BUCKET_FLUID.get(),net.neoforged.neoforge.fluids.SimpleFluidContent.copyOf(new FluidStack(Fluids.WATER,4000)));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(absolute).add(0,.5,0),Direction.UP,absolute,false);
        var result=CopperDynamics.COPPER_BUCKET.get().placeAtHit(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        h.assertTrue(result.getResult().consumesAction(),"Creative world placement failed");
        h.assertTrue(h.getLevel().getFluidState(absolute.above()).is(Fluids.WATER),"World fluid was not placed");
        h.assertTrue(CopperBucketItem.contents(player.getMainHandItem()).getAmount()==4000,"Creative placement consumed copper bucket fluid");
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        h.succeed();
    }
    @GameTest(template="empty")
    public static void horizontalTankSquareCrossSection(GameTestHelper h) {
        var start=new BlockPos(1,2,1);
        var state=CopperDynamics.HORIZONTAL_SMART_TANK.get().defaultBlockState().setValue(HorizontalSmartTankBlock.AXIS,Direction.Axis.Z);
        for(int x=0;x<2;x++) for(int y=0;y<2;y++) for(int z=0;z<3;z++) h.setBlock(start.offset(x,y,z),state);
        h.runAfterDelay(6,()->{
            var tank=(HorizontalSmartTankBlockEntity)h.getBlockEntity(start);
            var root=tank.getControllerBE();
            h.assertTrue(root!=null && root.getWidth()==2 && root.getHeight()==3,"Horizontal 2x2 cross-section did not merge");
            h.assertTrue(root.inventory.capacity()==12*com.simibubi.create.content.fluids.tank.FluidTankBlockEntity.getCapacityMultiplier(),"Horizontal volume wrong");
            h.succeed();
        });
    }
    @GameTest(template="empty")
    public static void horizontalTankJoinsAndSplits(GameTestHelper h) {
        var start=new BlockPos(1,2,1);
        var state=CopperDynamics.HORIZONTAL_SMART_TANK.get().defaultBlockState().setValue(HorizontalSmartTankBlock.AXIS,Direction.Axis.X);
        h.setBlock(start,state); h.setBlock(start.east(),state); h.setBlock(start.east(2),state);
        h.runAfterDelay(5,()->{
            var tank=(HorizontalSmartTankBlockEntity)h.getBlockEntity(start);
            h.assertTrue(tank.getControllerBE()!=null && tank.getControllerBE().getHeight()==3,"Horizontal length must merge");
            h.assertTrue(tank.inventory.fill(new FluidStack(Fluids.WATER,9000),FluidAction.EXECUTE)==9000,"Horizontal shared capacity missing");
            h.assertTrue(tank.inventory.fill(new FluidStack(Fluids.LAVA,1000),FluidAction.EXECUTE)==1000,"Multi-fluid storage missing");
            var end=(HorizontalSmartTankBlockEntity)h.getBlockEntity(start.east(2));
            h.assertTrue(end.inventory.amount()==10000,"Member did not forward to controller");
            var saved=tank.saveWithFullMetadata(h.getLevel().registryAccess());
            var restored=new HorizontalSmartTankBlockEntity(tank.getBlockPos(),state);
            restored.setLevel(h.getLevel()); restored.loadWithComponents(saved,h.getLevel().registryAccess());
            h.assertTrue(saved.contains("Fluids"),"Horizontal controller failed to save fluids");
            h.setBlock(start.east(),net.minecraft.world.level.block.Blocks.AIR);
            h.runAfterDelay(5,()->{
                var a=(HorizontalSmartTankBlockEntity)h.getBlockEntity(start);
                var b=(HorizontalSmartTankBlockEntity)h.getBlockEntity(start.east(2));
                h.assertTrue(a.inventory.amount()+b.inventory.amount()==10000,"Horizontal split lost fluid within surviving capacity");
                h.succeed();
            });
        });
    }
    @GameTest(template="empty")
    public static void droppedSealedCargoUsesPackageEntity(GameTestHelper h) {
        var stack=FluidPackageItem.containingFluid(new FluidStack(Fluids.WATER,1000));
        stack.set(CopperDynamics.RECOVERED.get(),true);
        var pos=h.absolutePos(new BlockPos(1,3,1));
        var item=new ItemEntity(h.getLevel(),pos.getX()+.5,pos.getY(),pos.getZ()+.5,stack);
        h.getLevel().addFreshEntity(item);
        var contents=new net.neoforged.neoforge.items.ItemStackHandler(9);
        contents.setStackInSlot(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT));
        var brass=CompositePackageItem.containing(contents,java.util.List.of(new FluidStack(Fluids.WATER,1000)));
        h.getLevel().addFreshEntity(new ItemEntity(h.getLevel(),pos.getX()+.5,pos.getY(),pos.getZ()+.5,brass));
        h.runAfterDelay(2,()->{
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(1),e->FluidPackageItem.isFluidPackage(e.getItem())).isEmpty(),"Dropped canister remained an item");
        h.assertTrue(!h.getLevel().getEntitiesOfClass(PackageEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(1),e->FluidPackageItem.isFluidPackage(e.box)).isEmpty(),"Dropped canister did not become package");
        h.assertTrue(!h.getLevel().getEntitiesOfClass(PackageEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(1),e->CompositePackageItem.isComposite(e.box)).isEmpty(),"Dropped brass box did not become package");
        h.succeed();
        });
    }
}
