package dev.ccd.content;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;

public final class CreativeDeliveryTankBlock extends DirectionalBlock implements IBE<CreativeDeliveryTankBlockEntity>, IWrenchable {
    public CreativeDeliveryTankBlock(Properties properties){super(properties);registerDefaultState(defaultBlockState().setValue(FACING,net.minecraft.core.Direction.UP));}
    @Override protected com.mojang.serialization.MapCodec<? extends DirectionalBlock> codec(){return simpleCodec(CreativeDeliveryTankBlock::new);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getClickedFace());}
    @Override public Class<CreativeDeliveryTankBlockEntity> getBlockEntityClass(){return CreativeDeliveryTankBlockEntity.class;}
    @Override public net.minecraft.world.level.block.entity.BlockEntityType<? extends CreativeDeliveryTankBlockEntity> getBlockEntityType(){return CopperDynamics.CREATIVE_DELIVERY_TANK_BE.get();}
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
        if(stack.is(net.neoforged.neoforge.common.Tags.Items.TOOLS_WRENCH))return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        var fluid=FluidStockItem.sample(stack);
        if(fluid.isEmpty()||!player.isCreative())return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof CreativeDeliveryTankBlockEntity tank)tank.select(fluid);
        return net.minecraft.world.ItemInteractionResult.SUCCESS;
    }
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){
        if(!player.isCreative()||!player.isShiftKeyDown())return net.minecraft.world.InteractionResult.PASS;
        if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof CreativeDeliveryTankBlockEntity tank)tank.select(net.neoforged.neoforge.fluids.FluidStack.EMPTY);
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
    }
}
