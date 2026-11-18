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
    @SubscribeEvent public static void join(EntityJoinLevelEvent event) {
        if(event.getLevel().isClientSide() || !(event.getEntity() instanceof ItemEntity item)
            || !CompositePackageItem.isSealed(item.getItem())
            || item.getPersistentData().getBoolean("CCDRecoveryDrop")) return;
        var box=PackageEntity.fromDroppedItem(event.getLevel(),item,item.getItem().copy());
        if(box==null) return;
        event.setCanceled(true);
        event.getLevel().addFreshEntity(box);
    }
}
