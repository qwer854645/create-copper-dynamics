package dev.ccd.compat;

import dev.ccd.CopperDynamics;
import dev.ccd.content.FluidPackageItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** Fill a detached one-item copy before committing either hand: never partially unpack. */
@EventBusSubscriber(modid=CopperDynamics.ID)
public final class HandheldPackageOpening {
    private HandheldPackageOpening() {}
    public static boolean applies(Player player) {
        if(!player.isShiftKeyDown()) return false;
        return FluidPackageItem.isFluidPackage(player.getMainHandItem())
            && player.getOffhandItem().getCapability(Capabilities.FluidHandler.ITEM)!=null
            || FluidPackageItem.isFluidPackage(player.getOffhandItem())
            && player.getMainHandItem().getCapability(Capabilities.FluidHandler.ITEM)!=null;
    }
    public static boolean unpack(Player player) {
        InteractionHand parcelHand=FluidPackageItem.isFluidPackage(player.getMainHandItem())?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;
        InteractionHand vesselHand=parcelHand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
        ItemStack parcel=player.getItemInHand(parcelHand),vessels=player.getItemInHand(vesselHand);
        if(!FluidPackageItem.isValid(parcel)) return false;
        var fluid=FluidPackageItem.contents(parcel);
        var candidate=vessels.copyWithCount(1);
        var handler=candidate.getCapability(Capabilities.FluidHandler.ITEM);
        if(handler==null || handler.fill(fluid.copy(),FluidAction.SIMULATE)!=fluid.getAmount()) return false;
        // All writes so far affect an isolated copy, even for handlers whose
        // execution accepts less than their simulation promised.
        if(handler.fill(fluid.copy(),FluidAction.EXECUTE)!=fluid.getAmount()) return false;
        ItemStack filled=handler.getContainer();
        if(filled.isEmpty()) return false;
        if(player.level().isClientSide()) return true;
        player.setItemInHand(parcelHand,ItemStack.EMPTY);
        if(vessels.getCount()==1) player.setItemInHand(vesselHand,filled);
        else {
            vessels.shrink(1);
            player.getInventory().placeItemBackInInventory(filled);
        }
        com.simibubi.create.AllSoundEvents.PACKAGE_POP.playOnServer(player.level(),player.blockPosition());
        return true;
    }
    private static boolean interact(PlayerInteractEvent event) {
        var player=event.getEntity();
        if(!applies(player)) return false;
        boolean success=unpack(player);
        if(!success && !player.level().isClientSide())
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("ccd.package.container_too_small"),true);
        return true;
    }
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void air(PlayerInteractEvent.RightClickItem event) {
        if(interact(event)) { event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS); }
    }
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void block(PlayerInteractEvent.RightClickBlock event) {
        if(interact(event)) { event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS); }
    }
}
