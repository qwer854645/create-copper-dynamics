package dev.ccd.content;

import dev.ccd.CopperDynamics;
import dev.ccd.compat.ProcessingMedia;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Powder-snow-like exposure timing, using heat damage rather than the freezing state. */
@EventBusSubscriber(modid=CopperDynamics.ID)
public final class SuperLavaScalding {
    public static final int THRESHOLD=100, DAMAGE_INTERVAL=10;
    private static final String EXPOSURE="ccdSuperLavaExposure";
    private SuperLavaScalding(){}
    public static boolean hot(ItemStack stack){
        var lava=ProcessingMedia.ALL.getFirst();
        if(stack.is(lava.bucket.get()))return true;
        if(!(stack.getItem() instanceof CopperBucketItem))return false;
        var fluid=CopperBucketItem.contents(stack);
        return !fluid.isEmpty()&&fluid.getFluid().getFluidType()==lava.type.get();
    }
    public static int exposure(Player player){return player.getPersistentData().getInt(EXPOSURE);}
    public static void tick(Player player){
        if(player.level().isClientSide())return;
        boolean immune=player.isCreative()||player.isSpectator()||player.isDeadOrDying();
        boolean holding=!immune&&(hot(player.getMainHandItem())||hot(player.getOffhandItem()));
        int previous=Math.clamp(exposure(player),0,THRESHOLD);
        int next=immune?0:holding?Math.min(THRESHOLD,previous+1):Math.max(0,previous-2);
        if(next==0)player.getPersistentData().remove(EXPOSURE);
        else player.getPersistentData().putInt(EXPOSURE,next);
        if(holding&&next==THRESHOLD&&player.tickCount%DAMAGE_INTERVAL==0)
            player.hurt(player.damageSources().hotFloor(),4);
    }
    @SubscribeEvent public static void afterPlayerTick(PlayerTickEvent.Post event){tick(event.getEntity());}
}
