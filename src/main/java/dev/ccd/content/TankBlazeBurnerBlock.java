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
    @Override public com.simibubi.create.content.schematics.requirement.ItemRequirement getRequiredItems(BlockState state,net.minecraft.world.level.block.entity.BlockEntity be){return new com.simibubi.create.content.schematics.requirement.ItemRequirement(com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType.CONSUME,new net.minecraft.world.item.ItemStack(CopperDynamics.TANK_BURNER_ITEM.get()));}
}
