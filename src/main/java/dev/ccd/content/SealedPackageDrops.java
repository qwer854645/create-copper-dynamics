package dev.ccd.content;

import com.simibubi.create.content.logistics.box.PackageEntity;
import dev.ccd.CopperDynamics;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Player drops also pass this entry point, unlike ItemEntity's spawn helper replacement. */
@EventBusSubscriber(modid=CopperDynamics.ID)
public final class SealedPackageDrops {
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGHEST) public static void join(EntityJoinLevelEvent event) {
        if(event.getLevel().isClientSide() || !(event.getEntity() instanceof ItemEntity item)
            || !CompositePackageItem.isSealed(item.getItem())
            || item.getPersistentData().getBoolean("CCDRecoveryDrop")) return;
        // Avoid two independent replacements by the native HIGH-priority handler.
        var box=PackageEntity.fromItemStack(event.getLevel(),item.position(),item.getItem().copy());
        if(box==null) return;
        box.setDeltaMovement(item.getDeltaMovement());
        event.setCanceled(true);
        event.getLevel().addFreshEntity(box);
    }
}
