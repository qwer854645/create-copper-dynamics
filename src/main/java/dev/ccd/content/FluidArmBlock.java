package dev.ccd.content;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlock;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.AllItems;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class FluidArmBlock extends ArmBlock {
    public FluidArmBlock(Properties properties) { super(properties); }
    @Override public BlockEntityType<? extends ArmBlockEntity> getBlockEntityType() {
        return CopperDynamics.FLUID_ARM_BE.get();
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
        BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(net.neoforged.neoforge.common.Tags.Items.TOOLS_WRENCH))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.getBlockEntity(pos) instanceof FluidArmBlockEntity arm
            && !(AllItems.GOGGLES.isIn(stack) && !arm.wearingGoggles())
            && arm.unloadTo(player)) return ItemInteractionResult.SUCCESS;
        // Preserve native first-time goggles attachment, including while carrying fluid.
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
}
