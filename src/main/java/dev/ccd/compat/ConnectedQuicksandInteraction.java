package dev.ccd.compat;

import dev.ccd.CCDFluids;
import dev.ccd.CopperDynamics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** Optional registry-only integration; no Connected classes required on the classpath. */
@EventBusSubscriber(modid=CopperDynamics.ID)
public final class ConnectedQuicksandInteraction {
    public static final ResourceLocation EMPTY=ResourceLocation.fromNamespaceAndPath("create_connected","empty_fan_catalyst");
    public static final ResourceLocation SANDING=ResourceLocation.fromNamespaceAndPath("create_connected","fan_sanding_catalyst");

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void apply(PlayerInteractEvent.RightClickBlock event) {
        if(!ModList.get().isLoaded("create_connected")) return;
        var level=event.getLevel(); var pos=event.getPos(); var player=event.getEntity();
        var state=level.getBlockState(pos);
        if(!BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(EMPTY)) return;
        var stack=event.getItemStack();
        // Work on a private container copy. Failed execution cannot consume the held stack.
        var working=stack.copyWithCount(1);
        var handler=working.getCapability(Capabilities.FluidHandler.ITEM);
        if(handler==null) return;
        var sample=handler.drain(1000,FluidAction.SIMULATE);
        if(sample.getAmount()!=1000) return;
        var catalyst=FanCompat.catalyst(sample);
        if(catalyst==null) return;
        var target=ResourceLocation.fromNamespaceAndPath("create_connected",catalyst);
        if(!BuiltInRegistries.BLOCK.containsKey(target) || !featureEnabled(target)) return;
        var requested=sample.copy();
        var planned=handler.drain(requested.copy(),FluidAction.SIMULATE);
        if(!FluidStack.matches(planned,requested)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if(level.isClientSide() || !level.mayInteract(player,pos)
            || !player.mayUseItemAt(pos,event.getFace(),stack)) return;
        // Preserve compatible direction/waterlogging properties from the empty catalyst.
        var next=BuiltInRegistries.BLOCK.get(target).withPropertiesOf(state);
        if(!next.canSurvive(level,pos)) return;
        if(!player.isCreative() && !FluidStack.matches(handler.drain(requested,FluidAction.EXECUTE),requested)) return;
        if(!level.setBlock(pos,next,Block.UPDATE_ALL)) return;
        if(!player.isCreative()) {
            if(stack.getCount()==1) player.setItemInHand(event.getHand(),handler.getContainer());
            else { stack.shrink(1); player.getInventory().placeItemBackInInventory(handler.getContainer()); }
        }
        level.playSound(null,pos,SoundEvents.BUCKET_EMPTY,SoundSource.BLOCKS,1,1);
        level.gameEvent(player,GameEvent.BLOCK_CHANGE,pos);
    }
    private static boolean featureEnabled(ResourceLocation id) {
        try {
            var type=Class.forName("com.hlysine.create_connected.config.FeatureToggle");
            return (boolean)type.getMethod("isEnabled",ResourceLocation.class).invoke(null,id);
        } catch(ReflectiveOperationException | LinkageError exception) {
            // Unknown Connected versions must not bypass disabled features.
            return false;
        }
    }
}
