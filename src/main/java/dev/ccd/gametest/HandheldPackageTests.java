package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.compat.HandheldPackageOpening;
import dev.ccd.content.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccd")
@PrefixGameTestTemplate(false)
public class HandheldPackageTests {
    @GameTest(template="empty")
    public static void brassBucketHoldsEightBucketsAndAcceptsTwoParcels(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(CopperDynamics.BRASS_BUCKET.get()));
        for(int i=0;i<2;i++) {
            player.setItemInHand(InteractionHand.MAIN_HAND,FluidPackageItem.containingFluid(new FluidStack(Fluids.WATER,4000)));
            h.assertTrue(HandheldPackageOpening.unpack(player),"Brass bucket rejected parcel");
        }
        var full=player.getOffhandItem().copy();
        h.assertTrue(CopperBucketItem.contents(full).getAmount()==8000,"Incorrect brass capacity");
        player.setItemInHand(InteractionHand.MAIN_HAND,FluidPackageItem.containingFluid(new FluidStack(Fluids.WATER,1)));
        h.assertTrue(!HandheldPackageOpening.unpack(player) && ItemStack.matches(full,player.getOffhandItem()),"Full brass bucket changed");
        var handler=full.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
        h.assertTrue(handler.getTankCapacity(0)==8000,"Wrong item capability capacity");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void wholeParcelFitsOrNothingChanges(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.setShiftKeyDown(true);
        for(var parcelHand:InteractionHand.values()) {
            var vesselHand=parcelHand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
            var parcel=FluidPackageItem.containingFluid(new FluidStack(Fluids.WATER,4000));
            player.setItemInHand(parcelHand,parcel.copy());
            player.setItemInHand(vesselHand,new ItemStack(Items.BUCKET));
            h.assertTrue(!HandheldPackageOpening.unpack(player),"Small bucket accepted partial parcel");
            h.assertTrue(ItemStack.matches(parcel,player.getItemInHand(parcelHand)) && player.getItemInHand(vesselHand).is(Items.BUCKET),"Failure changed hands");
            player.setItemInHand(vesselHand,new ItemStack(CopperDynamics.COPPER_BUCKET.get()));
            h.assertTrue(HandheldPackageOpening.unpack(player),"Copper bucket rejected full parcel");
            h.assertTrue(player.getItemInHand(parcelHand).isEmpty(),"Parcel not consumed");
            h.assertTrue(CopperBucketItem.contents(player.getItemInHand(vesselHand)).getAmount()==4000,"Wrong filled amount");
            player.setItemInHand(parcelHand,FluidPackageItem.containingFluid(new FluidStack(Fluids.WATER,1000)));
            player.setItemInHand(vesselHand,new ItemStack(Items.BUCKET));
            h.assertTrue(HandheldPackageOpening.unpack(player) && player.getItemInHand(vesselHand).is(Items.WATER_BUCKET),"Native bucket not replaced");
        }
        h.succeed();
    }
}
