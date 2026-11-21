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
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){var connection=PumpPlacement.preferredConnection(context);return defaultBlockState().setValue(FACING,connection==null?context.getClickedFace():connection);}
    @Override public BlockState getRotatedBlockState(BlockState state,net.minecraft.core.Direction clickedFace){
        var facing=state.getValue(FACING);
        // Clicking the outlet face must also work when it is parallel to the rotation axis.
        var axis=clickedFace.getAxis()==facing.getAxis()
            ?(facing.getAxis()==net.minecraft.core.Direction.Axis.Y?net.minecraft.core.Direction.Axis.X:net.minecraft.core.Direction.Axis.Y)
            :clickedFace.getAxis();
        return state.setValue(FACING,facing.getClockWise(axis));
    }
    @Override public BlockState rotate(BlockState state,net.minecraft.world.level.block.Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public BlockState mirror(BlockState state,net.minecraft.world.level.block.Mirror mirror){return state.setValue(FACING,mirror.mirror(state.getValue(FACING)));}
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){
        super.onPlace(state,level,pos,old,moving);
        if(!level.isClientSide()&&old.is(this)&&state.getValue(FACING)!=old.getValue(FACING)){
            level.invalidateCapabilities(pos);
            for(var side:new net.minecraft.core.Direction[]{old.getValue(FACING),state.getValue(FACING)}){
                var neighbour=pos.relative(side);
                if(level.hasChunkAt(neighbour))com.simibubi.create.content.fluids.FluidPropagator.propagateChangedPipe(level,neighbour,level.getBlockState(neighbour));
            }
        }
    }
    @Override public Class<CreativeDeliveryTankBlockEntity> getBlockEntityClass(){return CreativeDeliveryTankBlockEntity.class;}
    @Override public net.minecraft.world.level.block.entity.BlockEntityType<? extends CreativeDeliveryTankBlockEntity> getBlockEntityType(){return CopperDynamics.CREATIVE_DELIVERY_TANK_BE.get();}
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
        if(stack.is(net.neoforged.neoforge.common.Tags.Items.TOOLS_WRENCH)){
            var context=new net.minecraft.world.item.context.UseOnContext(player,hand,hit);
            var result=player.isShiftKeyDown()?onSneakWrenched(state,context):onWrenched(state,context);
            return result.consumesAction()?net.minecraft.world.ItemInteractionResult.SUCCESS:net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        var fluid=FluidStockItem.sample(stack);
        if(fluid.isEmpty()||!player.isCreative())return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof CreativeDeliveryTankBlockEntity tank)tank.select(fluid);
        return net.minecraft.world.ItemInteractionResult.SUCCESS;
    }
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){
        if(!player.isCreative()||!player.isShiftKeyDown()||!player.getMainHandItem().isEmpty())return net.minecraft.world.InteractionResult.PASS;
        if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof CreativeDeliveryTankBlockEntity tank)tank.select(net.neoforged.neoforge.fluids.FluidStack.EMPTY);
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
    }
}
