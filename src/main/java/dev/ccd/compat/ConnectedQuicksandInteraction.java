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
        if(!BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(EMPTY)
            || !BuiltInRegistries.BLOCK.containsKey(SANDING)) return;
        var stack=event.getItemStack();
        var handler=stack.getCapability(Capabilities.FluidHandler.ITEM);
        if(handler==null) return;
        var requested=new FluidStack(CCDFluids.QUICKSAND.get(),1000);
        var planned=handler.drain(requested.copy(),FluidAction.SIMULATE);
        if(!FluidStack.matches(planned,requested)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if(level.isClientSide() || !level.mayInteract(player,pos)
            || !player.mayUseItemAt(pos,event.getFace(),stack)) return;
        // Preserve compatible direction/waterlogging properties from the empty catalyst.
        var next=BuiltInRegistries.BLOCK.get(SANDING).withPropertiesOf(state);
        if(!next.canSurvive(level,pos) || !level.setBlock(pos,next,Block.UPDATE_ALL)) return;
        if(!player.isCreative()) {
            handler.drain(requested,FluidAction.EXECUTE);
            player.setItemInHand(event.getHand(),handler.getContainer());
        }
        level.playSound(null,pos,SoundEvents.BUCKET_EMPTY,SoundSource.BLOCKS,1,1);
        level.gameEvent(player,GameEvent.BLOCK_CHANGE,pos);
    }
}
