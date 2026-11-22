package dev.ccd.content;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class TankBlazeBurnerBlock extends BlazeBurnerBlock {
    public TankBlazeBurnerBlock(Properties properties){super(properties);registerDefaultState(defaultBlockState().setValue(HEAT_LEVEL,HeatLevel.SMOULDERING));}
    @Override public BlockEntityType<? extends BlazeBurnerBlockEntity> getBlockEntityType(){return CopperDynamics.TANK_BURNER_BE.get();}
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new TankBlazeBurnerBlockEntity(pos,state);}
    @Override public net.minecraft.world.item.ItemStack getCloneItemStack(BlockState state,net.minecraft.world.phys.HitResult hit,net.minecraft.world.level.LevelReader level,BlockPos pos,net.minecraft.world.entity.player.Player player){return new net.minecraft.world.item.ItemStack(CopperDynamics.TANK_BURNER_ITEM.get());}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
        if(stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM)!=null){
            if(level.isClientSide())return net.minecraft.world.ItemInteractionResult.SUCCESS;
            boolean transferred=net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(player,hand,level,pos,hit.getDirection());
            return transferred?net.minecraft.world.ItemInteractionResult.SUCCESS:net.minecraft.world.ItemInteractionResult.FAIL;
        }
        return super.useItemOn(stack,state,level,pos,player,hand,hit);
    }
    @Override public com.simibubi.create.content.schematics.requirement.ItemRequirement getRequiredItems(BlockState state,net.minecraft.world.level.block.entity.BlockEntity be){return new com.simibubi.create.content.schematics.requirement.ItemRequirement(com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType.CONSUME,new net.minecraft.world.item.ItemStack(CopperDynamics.TANK_BURNER_ITEM.get()));}
}
