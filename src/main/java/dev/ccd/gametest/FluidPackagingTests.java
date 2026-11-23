package dev.ccd.gametest;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.repackager.PackageRepackageHelper;
import dev.ccd.CopperDynamics;
import dev.ccd.content.FluidCargo;
import dev.ccd.content.FluidPackageItem;
import dev.ccd.content.FluidPackagerBlockEntity;
import dev.ccd.content.FluidTransfers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CopperDynamics.ID)
@PrefixGameTestTemplate(false)
public final class FluidPackagingTests {
    private static FluidStack water(int amount) { return new FluidStack(Fluids.WATER, amount); }
    private static void check(GameTestHelper helper, boolean condition, String message) { helper.assertTrue(condition, message); }

    @GameTest(template = "empty")
    public static void packageIdentityAndRandomStyles(GameTestHelper h) {
        ItemStack box = FluidPackageItem.containingFluid(water(250));
        check(h, PackageItem.isPackage(box), "Fluid package must be a native package");
        check(h, !PackageStyles.ALL_BOXES.contains(box.getItem()), "Must not replace Create's default ordinary box");
        check(h, !PackageStyles.STANDARD_BOXES.contains(box.getItem()), "Must not become an ordinary random box");
        check(h, !PackageStyles.RARE_BOXES.contains(box.getItem()), "Must not enter rare random styles");
        for (int i = 0; i < 100; i++) check(h, !FluidPackageItem.isFluidPackage(PackageStyles.getRandomBox()), "Random item box was fluid-only");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void cargoIsDefensive(GameTestHelper h) {
        FluidStack original = water(750);
        FluidCargo cargo = new FluidCargo(original);
        original.setAmount(1);
        cargo.fluid().setAmount(2);
        check(h, cargo.fluid().getAmount() == 750, "Mutable FluidStack escaped immutable component");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void savePreservesFluidComponentsAndAddress(GameTestHelper h) {
        FluidStack fluid = water(333);
        fluid.set(DataComponents.CUSTOM_NAME, Component.literal("distinct fluid attributes"));
        ItemStack box = FluidPackageItem.containingFluid(fluid);
        PackageItem.addAddress(box, "Mixing Hall");
        var registries = h.getLevel().registryAccess();
        ItemStack restored = ItemStack.parseOptional(registries, (net.minecraft.nbt.CompoundTag) box.saveOptional(registries));
        check(h, FluidStack.matches(fluid, FluidPackageItem.contents(restored)), "Fluid component changed during save/load");
        check(h, PackageItem.getAddress(restored).equals("Mixing Hall"), "Address lost");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void simulationDoesNotDrain(GameTestHelper h) {
        FluidTank tank = new FluidTank(4_000);
        tank.fill(water(750), FluidAction.EXECUTE);
        check(h, FluidTransfers.planDrain(tank, FluidStack.EMPTY, 4_000, false).getAmount() == 750, "Auto package should allow remainder");
        check(h, tank.getFluidAmount() == 750, "Planning consumed source fluid");
        check(h, FluidTransfers.planDrain(tank, FluidStack.EMPTY, 1_000, true).isEmpty(), "Exact mode accepted too little fluid");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void filteringRespectsComponents(GameTestHelper h) {
        FluidStack special = water(1_000);
        special.set(DataComponents.CUSTOM_NAME, Component.literal("special"));
        FluidTank tank = new FluidTank(4_000);
        tank.fill(special, FluidAction.EXECUTE);
        check(h, FluidTransfers.planDrain(tank, water(1), 1_000, false).isEmpty(), "Different fluid attributes must not match");
        check(h, FluidTransfers.planDrain(tank, special, 1_000, true).getAmount() == 1_000, "Exact matching filter rejected");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void targetMustFitWholePackage(GameTestHelper h) {
        FluidTank tank = new FluidTank(600);
        check(h, !FluidTransfers.canFillAll(tank, water(1_000)), "Insufficient target capacity accepted");
        check(h, tank.isEmpty(), "Simulation modified target");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void changedTargetRetainsRemainder(GameTestHelper h) {
        FluidTank target = new FluidTank(4_000) {
            @Override public int fill(FluidStack resource, FluidAction action) {
                return super.fill(action.simulate() ? resource : resource.copyWithAmount(Math.min(250, resource.getAmount())), action);
            }
        };
        check(h, FluidTransfers.canFillAll(target, water(1_000)), "Preflight should accept 1000");
        FluidStack rest = FluidTransfers.fillAndKeepRemainder(target, water(1_000));
        check(h, target.getFluidAmount() == 250 && rest.getAmount() == 750, "Actual accepted plus remainder must equal original");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void nativeUnpackerRejectsFluid(GameTestHelper h) {
        BlockPos local = new BlockPos(1, 1, 1);
        h.setBlock(local, AllBlocks.PACKAGER.getDefaultState());
        PackagerBlockEntity be = (PackagerBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(local));
        ItemStack box = FluidPackageItem.containingFluid(water(1_000));
        check(h, !be.unwrapBox(box, true), "Native item unpacker would void fluid cargo");
        check(h, be.inventory.insertItem(0, box, false).getCount() == 1, "Rejected package must remain in caller inventory");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void nativeRepackingKeepsFluidFragments(GameTestHelper h) {
        PackageRepackageHelper helper = new dev.ccd.logistics.CompositeRepackageHelper(null);
        ItemStack fluid = FluidPackageItem.containingFluid(water(1_000));
        PackageItem.setOrder(fluid, 42, 0, true, 0, false, null);
        check(h, helper.addPackageFragment(fluid) == -1, "Incomplete native fragment chain must wait");
        ItemStack last = FluidPackageItem.containingFluid(water(500));
        PackageItem.setOrder(last, 42, 0, true, 1, true, null);
        check(h, helper.addPackageFragment(last) == 42, "Complete native fragment chain was not accepted");
        var output = helper.repack(42, h.getLevel().random);
        check(h, output.size() == 1 && dev.ccd.content.CompositePackageItem.isComposite(output.getFirst().stack), "Expected composite parcel");
        check(h, dev.ccd.content.CompositePackageItem.fluids(output.getFirst().stack).getFirst().getAmount() == 1_500,
            "Repacking changed total fluid quantity");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fluidRepackingPreservesNativeRecipeGroups(GameTestHelper h) {
        var slots = new net.neoforged.neoforge.items.ItemStackHandler(PackageItem.SLOTS);
        slots.setStackInSlot(0, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 8));
        slots.setStackInSlot(1, new ItemStack(net.minecraft.world.item.Items.REDSTONE, 4));
        var context = com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts.singleRecipe(java.util.List.of(
            new com.simibubi.create.content.logistics.BigItemStack(new ItemStack(net.minecraft.world.item.Items.IRON_INGOT), 2),
            new com.simibubi.create.content.logistics.BigItemStack(new ItemStack(net.minecraft.world.item.Items.REDSTONE), 1)));
        ItemStack itemParcel = PackageItem.containing(slots);
        PackageItem.setOrder(itemParcel, 88, 0, true, 0, false, context);
        ItemStack fluidParcel = FluidPackageItem.containingFluid(water(1000));
        PackageItem.setOrder(fluidParcel, 88, 0, true, 1, true, null);
        var nativeHelper = new PackageRepackageHelper();
        var fluidHelper = new dev.ccd.logistics.CompositeRepackageHelper(null);
        nativeHelper.addPackageFragment(itemParcel); nativeHelper.addPackageFragment(fluidParcel);
        fluidHelper.addPackageFragment(itemParcel); fluidHelper.addPackageFragment(fluidParcel);
        var expected = new java.util.ArrayList<net.minecraft.world.item.component.ItemContainerContents>();
        for (var entry : nativeHelper.repack(88, net.minecraft.util.RandomSource.create(0)))
            for (int i = 0; i < entry.count; i++) expected.add(entry.stack.get(com.simibubi.create.AllDataComponents.PACKAGE_CONTENTS));
        var actual = new java.util.ArrayList<net.minecraft.world.item.component.ItemContainerContents>();
        for (var entry : fluidHelper.repack(88, net.minecraft.util.RandomSource.create(0)))
            for (int i = 0; i < entry.count; i++) actual.add(entry.stack.get(com.simibubi.create.AllDataComponents.PACKAGE_CONTENTS));
        check(h, expected.equals(actual), "Fluid order changed native recipe item grouping");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void overlappingAccessIsNotIdenticalInventory(GameTestHelper h) {
        BlockPos machine = new BlockPos(2,1,2);
        h.setBlock(machine.north(), AllBlocks.FLUID_TANK.getDefaultState());
        h.setBlock(machine, CopperDynamics.FLUID_PACKAGER.get().defaultBlockState().setValue(PackagerBlock.FACING,Direction.SOUTH));
        h.runAfterDelay(3, () -> {
            var be = (FluidPackagerBlockEntity) h.getBlockEntity(machine);
            var own = be.targetInventory.getIdentifiedInventory();
            var id = (dev.ccd.logistics.FluidInventoryBehaviour.AccessId) own.identifier();
            var larger = new java.util.HashSet<BlockPos>(id.sources());
            larger.add(h.absolutePos(machine.east(3)));
            var overlap = new com.simibubi.create.content.logistics.packager.IdentifiedInventory(
                new dev.ccd.logistics.FluidInventoryBehaviour.AccessId(id.dimension(),larger,id.access()),
                be.targetInventory.getInventory());
            check(h, be.isTargetingSameInventory(own), "Identical source rejected");
            check(h, !be.isTargetingSameInventory(overlap), "Partial overlap treated as identical inventory");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void packageAndUnpackAgainstCreateTank(GameTestHelper h) {
        BlockPos machine = new BlockPos(2, 1, 2);
        BlockPos tank = machine.north();
        h.setBlock(tank, AllBlocks.FLUID_TANK.getDefaultState());
        h.setBlock(machine, CopperDynamics.FLUID_PACKAGER.get().defaultBlockState().setValue(PackagerBlock.FACING, Direction.SOUTH));
        h.runAfterDelay(3, () -> {
            var level = h.getLevel();
            var be = (FluidPackagerBlockEntity) level.getBlockEntity(h.absolutePos(machine));
            var handler = level.getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(tank), Direction.SOUTH);
            check(h, handler != null, "Create tank did not expose standard fluid capability");
            handler.fill(water(1_250), FluidAction.EXECUTE);
            be.attemptToSend(null);
            check(h, FluidPackageItem.contents(be.heldBox).getAmount() == 1_250, "Machine should package all available fluid below capacity");
            check(h, handler.getFluidInTank(0).isEmpty(), "Wrong source remainder");
            ItemStack parcel = be.heldBox.copy();
            be.heldBox = ItemStack.EMPTY;
            be.animationTicks = 0;
            check(h, be.unwrapBox(parcel, true), "Own packager should accept fluid parcel");
            check(h, handler.getFluidInTank(0).isEmpty(), "Simulated unpack changed tank");
            check(h, be.inventory.insertItem(0, parcel, false).isEmpty(), "Native package item interface rejected own parcel");
            check(h, handler.getFluidInTank(0).getAmount() == 1_250, "Roundtrip lost or duplicated fluid");
            check(h, be.getRecovery().isEmpty(), "Unexpected retained fluid after full transfer");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void automaticModePackagesAvailableRemainder(GameTestHelper h) {
        BlockPos machine = new BlockPos(2, 1, 2);
        h.setBlock(machine.north(), AllBlocks.FLUID_TANK.getDefaultState());
        h.setBlock(machine, CopperDynamics.FLUID_PACKAGER.get().defaultBlockState().setValue(PackagerBlock.FACING, Direction.SOUTH));
        h.runAfterDelay(3, () -> {
            var be = (FluidPackagerBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(machine));
            var handler = be.targetFluidHandler();
            check(h, handler != null, "Missing tank capability");
            handler.fill(water(250), FluidAction.EXECUTE);
            be.attemptToSend(null);
            check(h, FluidPackageItem.contents(be.heldBox).getAmount() == 250, "Machine should package the available remainder");
            check(h, handler.getFluidInTank(0).isEmpty(), "Packaged fluid must leave the source");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void capabilityDisappearingAfterPreflightIsSaved(GameTestHelper h) {
        BlockPos machine = new BlockPos(2, 1, 2);
        h.setBlock(machine.north(), AllBlocks.FLUID_TANK.getDefaultState());
        var state = CopperDynamics.FLUID_PACKAGER.get().defaultBlockState().setValue(PackagerBlock.FACING, Direction.SOUTH);
        h.setBlock(machine, state);
        h.runAfterDelay(3, () -> {
            var be = (FluidPackagerBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(machine));
            ItemStack box = FluidPackageItem.containingFluid(water(1_000));
            check(h, be.unwrapBox(box, true), "Initial empty tank should accept the parcel");
            h.setBlock(machine.north(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            check(h, be.unwrapBox(box, false), "Execution must take ownership after capability disappears");
            check(h, be.getRecovery().getAmount() == 1_000, "Missing target lost fluid");
            var registries = h.getLevel().registryAccess();
            var saved = be.saveWithFullMetadata(registries);
            var restored = new FluidPackagerBlockEntity(h.absolutePos(machine), state);
            restored.setLevel(h.getLevel());
            restored.loadWithComponents(saved, registries);
            check(h, restored.getRecovery().getAmount() == 1_000, "Retained fluid did not survive save/load");
            check(h, !restored.unwrapBox(box, true), "Retained fluid must block another incoming parcel");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void brassParcelSeparatesIntoItemsAndCopperParcels(GameTestHelper h) {
        BlockPos machine = new BlockPos(2,1,2);
        h.setBlock(machine.north(), net.minecraft.world.level.block.Blocks.BARREL.defaultBlockState());
        h.setBlock(machine, CopperDynamics.FLUID_PACKAGER.get().defaultBlockState().setValue(PackagerBlock.FACING,Direction.SOUTH));
        h.runAfterDelay(3, () -> {
            var be = (FluidPackagerBlockEntity) h.getBlockEntity(machine);
            var target = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,h.absolutePos(machine.north()),Direction.SOUTH);
            var slots = new net.neoforged.neoforge.items.ItemStackHandler(PackageItem.SLOTS);
            slots.setStackInSlot(0,new ItemStack(net.minecraft.world.item.Items.IRON_INGOT,3));
            var box = dev.ccd.content.CompositePackageItem.containing(slots,java.util.List.of(water(6500),
                new FluidStack(Fluids.LAVA,1000)));
            PackageItem.addAddress(box,"Fluid Hall");
            check(h,be.unwrapBox(box,true),"Item container should accept brass parcel separation");
            check(h,target.getStackInSlot(0).isEmpty() && be.queuedExitingPackages.isEmpty(),"Preflight changed cargo");
            check(h,be.inventory.insertItem(0,box,false).isEmpty(),"Package interface rejected separation");
            check(h,target.getStackInSlot(0).getCount()==3,"Items did not reach barrel");
            check(h,be.queuedExitingPackages.size()==3,"Expected two water parcels and one lava parcel");
            check(h,be.queuedExitingPackages.stream().mapToInt(e->FluidPackageItem.contents(e.stack).getAmount()).sum()==7500,"Separated fluid total changed");
            check(h,be.queuedExitingPackages.stream().allMatch(e->FluidPackageItem.contents(e.stack).getAmount()<=4000
                && PackageItem.getAddress(e.stack).equals("Fluid Hall")),"Copper parcel capacity or address changed");
            var saved = be.saveWithFullMetadata(h.getLevel().registryAccess());
            be.loadWithComponents(saved,h.getLevel().registryAccess());
            check(h,be.queuedExitingPackages.size()==3,"Output queue did not survive reload");
            h.setBlock(machine,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            var drops=h.getLevel().getEntitiesOfClass(com.simibubi.create.content.logistics.box.PackageEntity.class,
                new net.minecraft.world.phys.AABB(h.absolutePos(machine)).inflate(2));
            check(h,drops.stream().mapToInt(e->FluidPackageItem.contents(e.box).getAmount()).sum()==7500,"Breaking separator lost queued fluids");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void fullItemDestinationWaitsAndRetainsExecutionRemainder(GameTestHelper h) {
        BlockPos machine = new BlockPos(2,1,2);
        h.setBlock(machine.north(),net.minecraft.world.level.block.Blocks.BARREL.defaultBlockState());
        h.setBlock(machine,CopperDynamics.FLUID_PACKAGER.get().defaultBlockState().setValue(PackagerBlock.FACING,Direction.SOUTH));
        h.runAfterDelay(3, () -> {
            var be=(FluidPackagerBlockEntity)h.getBlockEntity(machine);
            var target=h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,h.absolutePos(machine.north()),Direction.SOUTH);
            var slots=new net.neoforged.neoforge.items.ItemStackHandler(PackageItem.SLOTS);
            slots.setStackInSlot(0,new ItemStack(net.minecraft.world.item.Items.IRON_INGOT,3));
            var box=dev.ccd.content.CompositePackageItem.containing(slots,java.util.List.of(water(1000)));
            check(h,be.unwrapBox(box,true),"Empty barrel should fit items");
            for(int slot=0;slot<target.getSlots();slot++)target.insertItem(slot,new ItemStack(net.minecraft.world.item.Items.STONE,64),false);
            check(h,!be.unwrapBox(box,true),"Full barrel should wait");
            check(h,be.queuedExitingPackages.isEmpty(),"Rejected simulation produced fluid parcels");
            check(h,be.unwrapBox(box,false),"Execution should own changed-destination cargo");
            check(h,PackageItem.getContents(be.getCompositeRecovery()).getStackInSlot(0).getCount()==3,"Item remainder lost");
            check(h,be.queuedExitingPackages.size()==1,"Fluid should be packaged once");
            be.lazyTick();
            check(h,be.queuedExitingPackages.size()==1,"Recovery duplicated fluid parcels");
            target.extractItem(0,64,false);
            be.lazyTick();
            check(h,be.getCompositeRecovery().isEmpty() && target.getStackInSlot(0).getCount()==3,"Recovery did not resume item delivery");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void compositeFluidUnloadsAndMissingItemsAreRetained(GameTestHelper h) {
        BlockPos machine = new BlockPos(2, 1, 2);
        h.setBlock(machine.north(), AllBlocks.FLUID_TANK.getDefaultState());
        h.setBlock(machine, CopperDynamics.FLUID_PACKAGER.get().defaultBlockState().setValue(PackagerBlock.FACING, Direction.SOUTH));
        h.runAfterDelay(3, () -> {
            var be = (FluidPackagerBlockEntity) h.getBlockEntity(machine);
            var items = new net.neoforged.neoforge.items.ItemStackHandler(PackageItem.SLOTS);
            var fluidOnly = dev.ccd.content.CompositePackageItem.containing(items, java.util.List.of(water(1500)));
            check(h, be.unwrapBox(fluidOnly, true), "Repacked fluid should fit Create tank");
            check(h, be.unwrapBox(fluidOnly, false), "Repacked fluid rejected");
            check(h, be.targetFluidHandler().getFluidInTank(0).getAmount() == 1500, "Composite fluid lost");
            be.animationTicks = 0;
            items.setStackInSlot(0, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 3));
            var mixed = dev.ccd.content.CompositePackageItem.containing(items, java.util.List.of(water(500)));
            check(h, !be.unwrapBox(mixed, true), "Tank without item interface must reject mixed preflight");
            check(h, be.unwrapBox(mixed, false), "Execution must retain cargo after target changes");
            check(h, PackageItem.getContents(be.getCompositeRecovery()).getStackInSlot(0).getCount() == 3, "Item remainder lost");
            check(h, be.targetFluidHandler().getFluidInTank(0).getAmount() == 2000, "Fluid remainder incorrect");
            var saved = be.saveWithFullMetadata(h.getLevel().registryAccess());
            var restored = new FluidPackagerBlockEntity(h.absolutePos(machine), be.getBlockState());
            restored.loadWithComponents(saved, h.getLevel().registryAccess());
            check(h, PackageItem.getContents(restored.getCompositeRecovery()).getStackInSlot(0).getCount() == 3, "Recovery items not saved");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void breakingPackagerReturnsRecovery(GameTestHelper h) {
        BlockPos machine = new BlockPos(2, 1, 2);
        h.setBlock(machine, CopperDynamics.FLUID_PACKAGER.get().defaultBlockState());
        var be = (FluidPackagerBlockEntity) h.getBlockEntity(machine);
        check(h, be.unwrapBox(FluidPackageItem.containingFluid(water(5_250)), false), "Failed to retain undelivered cargo");
        h.setBlock(machine, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        var boxes = h.getLevel().getEntitiesOfClass(com.simibubi.create.content.logistics.box.PackageEntity.class,
            new net.minecraft.world.phys.AABB(h.absolutePos(machine)).inflate(2));
        check(h, boxes.stream().mapToInt(e -> FluidPackageItem.contents(e.box).getAmount()).sum() == 5_250,
            "Breaking packager lost recovery fluid");
        check(h, boxes.stream().allMatch(e -> FluidPackageItem.contents(e.box).getAmount() <= FluidPackageItem.CAPACITY),
            "Recovery drop exceeded parcel capacity");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void nativeNetworkCodecPreservesFluid(GameTestHelper h) {
        ItemStack original = FluidPackageItem.containingFluid(water(731));
        PackageItem.addAddress(original, "receiver");
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            ItemStack.STREAM_CODEC.encode(buffer, original);
            ItemStack copy = ItemStack.STREAM_CODEC.decode(buffer);
            check(h, FluidStack.matches(FluidPackageItem.contents(copy), water(731)), "Network codec changed fluid quantity");
            check(h, PackageItem.getAddress(copy).equals("receiver"), "Network codec lost address");
        } finally { buffer.release(); }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void frogportExportsMachinePackageToChain(GameTestHelper h) {
        BlockPos machine = new BlockPos(1, 1, 1);
        BlockPos portPos = machine.above();
        BlockPos chainPos = new BlockPos(3, 1, 1);
        h.setBlock(machine, CopperDynamics.FLUID_PACKAGER.get().defaultBlockState());
        h.setBlock(portPos, AllBlocks.PACKAGE_FROGPORT.getDefaultState());
        h.setBlock(chainPos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        h.setBlock(chainPos, AllBlocks.CHAIN_CONVEYOR.getDefaultState());
        h.runAfterDelay(3, () -> {
            var level = h.getLevel();
            var be = (FluidPackagerBlockEntity) level.getBlockEntity(h.absolutePos(machine));
            var port = (com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity) level.getBlockEntity(h.absolutePos(portPos));
            var chain = (com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity) level.getBlockEntity(h.absolutePos(chainPos));
            ItemStack box = FluidPackageItem.containingFluid(water(1_250));
            PackageItem.addAddress(box, "destination");
            be.heldBox = box;
            be.animationTicks = 0;
            port.target = new com.simibubi.create.content.logistics.packagePort.PackagePortTarget.ChainConveyorFrogportTarget(
                chainPos.subtract(portPos), 0F, (BlockPos) null, false);
            port.addressFilter = "origin";
            chain.setSpeed(16);
            check(h, port.tryPullingFrom(be.inventory), "Native frogport did not extract the CCD parcel");
            check(h, be.heldBox.isEmpty(), "Frogport duplicated the source parcel");
            h.runAfterDelay(30, () -> {
                var packages = chain.getLoopingPackages();
                check(h, packages.size() == 1, "Parcel did not reach native chain conveyor");
                check(h, FluidPackageItem.contents(packages.getFirst().item).getAmount() == 1_250, "Chain lost fluid payload");
                check(h, PackageItem.getAddress(packages.getFirst().item).equals("destination"), "Chain lost package address");
                h.succeed();
            });
        });
    }
}
