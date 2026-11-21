package dev.ccd.content;

import net.createmod.catnip.placement.PlacementHelpers;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;

public final class BufferPumpBlockItem extends BlockItem {
    private final int helper=PlacementHelpers.register(new BufferPumpPlacementHelper());
    public BufferPumpBlockItem(BufferPumpBlock block,Properties properties){super(block,properties);}
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){
        var player=context.getPlayer();
        if(player==null||player.isShiftKeyDown()||!player.mayBuild())return InteractionResult.PASS;
        var level=context.getLevel();var state=level.getBlockState(context.getClickedPos());var placement=PlacementHelpers.get(helper);
        if(!placement.matchesState(state))return InteractionResult.PASS;
        var hit=new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(),context.getClickedFace(),context.getClickedPos(),context.isInside());
        var offset=placement.getOffset(player,level,state,context.getClickedPos(),hit);
        if(!offset.isSuccessful())return InteractionResult.PASS;
        return offset.placeInWorld(level,this,player,context.getHand(),hit).result();
    }
}
