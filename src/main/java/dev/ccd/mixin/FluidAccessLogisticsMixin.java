package dev.ccd.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.google.common.collect.Multimap;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import dev.ccd.logistics.FluidStockScope;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LogisticsManager.class)
abstract class FluidAccessLogisticsMixin {
    @WrapMethod(method="createSummaryOfNetwork")
    private static InventorySummary ccd$summary(UUID network,Operation<InventorySummary> original) {
        try(var scope=new FluidStockScope(null)) { return original.call(network); }
    }
    @WrapMethod(method="getStockOf")
    private static int ccd$stock(UUID network,ItemStack item,IdentifiedInventory excluded,Operation<Integer> original) {
        try(var scope=new FluidStockScope(excluded)) { return original.call(network,item,excluded); }
    }
    @WrapMethod(method="findPackagersForRequest")
    private static Multimap<PackagerBlockEntity,PackagingRequest> ccd$request(UUID network,PackageOrderWithCrafts order,
        IdentifiedInventory excluded,String address,Operation<Multimap<PackagerBlockEntity,PackagingRequest>> original) {
        try(var scope=new FluidStockScope(excluded)) { return original.call(network,order,excluded,address); }
    }
}
