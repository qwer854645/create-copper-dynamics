package dev.ccd.content;

import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.InteractionResult;

public final class PortableFluidPumpBlock extends ReciprocatingFluidPumpBlock {
    public PortableFluidPumpBlock(Properties properties) { super(properties); }
    @Override public BlockState getRotatedBlockState(BlockState state,net.minecraft.core.Direction clickedFace) {
        // Bellows stays upright: rotate the horizontal fluid/shaft axes together.
        return state.setValue(FACING,state.getValue(FACING).getClockWise());
    }
    @Override public net.minecraft.world.level.block.entity.BlockEntityType<? extends com.simibubi.create.content.fluids.pump.PumpBlockEntity> getBlockEntityType() {
        return CopperDynamics.PORTABLE_PUMP_BE.get();
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        return operate(level,pos,player,player.getMainHandItem())?InteractionResult.sidedSuccess(level.isClientSide()):InteractionResult.PASS;
    }
    private boolean operate(Level level,BlockPos pos,Player player,net.minecraft.world.item.ItemStack held) {
        if(player.isSpectator()||!player.mayBuild())return false;
        if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof PortableFluidPumpBlockEntity pump) {
            pump.turn(player.isShiftKeyDown());
            // Match the crank's Extendo Grip exemption and configured hunger cost.
            if(!held.is(com.simibubi.create.AllItems.EXTENDO_GRIP.get()))
                player.causeFoodExhaustion(32*com.simibubi.create.infrastructure.config.AllConfigs.server().kinetics.crankHungerMultiplier.getF());
            if(player.getFoodData().getFoodLevel()==0)
                com.simibubi.create.foundation.advancement.AllAdvancements.HAND_CRANK.awardTo(player);
        }
        return true;
    }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,net.minecraft.world.InteractionHand hand,BlockHitResult hit) {
        if(stack.is(net.neoforged.neoforge.common.Tags.Items.TOOLS_WRENCH)) {
            var context=new net.minecraft.world.item.context.UseOnContext(player,hand,hit);
            var result=player.isShiftKeyDown()?onSneakWrenched(state,context):onWrenched(state,context);
            return result.consumesAction()?net.minecraft.world.ItemInteractionResult.SUCCESS:net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return operate(level,pos,player,stack)?net.minecraft.world.ItemInteractionResult.SUCCESS
            :net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
