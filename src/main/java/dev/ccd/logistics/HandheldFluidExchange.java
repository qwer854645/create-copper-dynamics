package dev.ccd.logistics;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.fluid.FluidHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Native Create exchange order shared by explicit handheld-only endpoints. */
public final class HandheldFluidExchange {
    private HandheldFluidExchange(){}
    public static boolean emptyThenFill(Player player,InteractionHand hand,ItemStack stack,SmartBlockEntity owner){
        var level=owner.getLevel();
        if(level==null||level.isClientSide())return false;
        return FluidHelper.tryEmptyItemIntoBE(level,player,hand,stack,owner)
            ||FluidHelper.tryFillItemFromBE(level,player,hand,stack,owner);
    }
}
