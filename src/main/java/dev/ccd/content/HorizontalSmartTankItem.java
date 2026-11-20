package dev.ccd.content;

import com.simibubi.create.content.equipment.symmetryWand.SymmetryWandItem;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Native tank layer-placement rules, expressed in the horizontal controller's coordinates. */
public final class HorizontalSmartTankItem extends BlockItem {
    public HorizontalSmartTankItem(Block block, Properties properties) { super(block, properties); }

    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null) return null;
        var neighbour = context.getLevel().getBlockState(context.getClickedPos()
            .relative(context.getClickedFace().getOpposite()));
        if (neighbour.is(getBlock()))
            state = state.setValue(HorizontalSmartTankBlock.AXIS, neighbour.getValue(HorizontalSmartTankBlock.AXIS));
        return state;
    }

    @Override public InteractionResult place(BlockPlaceContext context) {
        var player = context.getPlayer();
        var level = context.getLevel();
        Direction face = context.getClickedFace();
        BlockPos placed = context.getClickedPos();
        var missing = new ArrayList<BlockPos>();
        // Snapshot the existing controller before placing the first block can merge it.
        if (player != null && !player.isShiftKeyDown() && !SymmetryWandItem.presentInHotbar(player)
            && level.getBlockEntity(placed.relative(face.getOpposite())) instanceof HorizontalSmartTankBlockEntity tank
            && tank.getBlockState().is(getBlock()) && face.getAxis() == tank.getMainConnectionAxis()) {
            var root = tank.getControllerBE();
            if (root instanceof HorizontalSmartTankBlockEntity horizontal && root.getWidth() > 1) {
                int layer = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? root.getHeight() : -1;
                BlockPos origin = root.getBlockPos().offset(horizontal.structureOffset(0, layer, 0));
                int targetCoordinate = face.getAxis().choose(origin.getX(), origin.getY(), origin.getZ());
                if (face.getAxis().choose(placed.getX(), placed.getY(), placed.getZ()) == targetCoordinate) {
                    for (int x = 0; x < root.getWidth(); x++) for (int z = 0; z < root.getWidth(); z++) {
                        BlockPos target = origin.offset(horizontal.structureOffset(x, 0, z));
                        BlockState existing = level.getBlockState(target);
                        if (existing.is(getBlock()) && existing.getValue(HorizontalSmartTankBlock.AXIS) == face.getAxis()) continue;
                        if (!existing.canBeReplaced() || !level.mayInteract(player, target)
                            || !player.mayUseItemAt(target, face, context.getItemInHand())) {
                            missing.clear();
                            return super.place(context);
                        }
                        missing.add(target);
                    }
                    // Include the first block in the same inventory check as the native algorithm.
                    if (!player.isCreative() && context.getItemInHand().getCount() < missing.size()) missing.clear();
                }
            }
        }
        InteractionResult result = super.place(context);
        if (!result.consumesAction() || player == null) return result;
        for (BlockPos target : missing) {
            if (target.equals(placed)) continue;
            if (context.getItemInHand().isEmpty() && !player.isCreative()) break;
            var placement = BlockPlaceContext.at(context, target, face);
            boolean wasSilent = player.getPersistentData().getBoolean("SilenceTankSound");
            player.getPersistentData().putBoolean("SilenceTankSound", true);
            try {
                if (!super.place(placement).consumesAction()) break;
            } finally {
                if (wasSilent) player.getPersistentData().putBoolean("SilenceTankSound", true);
                else player.getPersistentData().remove("SilenceTankSound");
            }
        }
        return result;
    }
}
