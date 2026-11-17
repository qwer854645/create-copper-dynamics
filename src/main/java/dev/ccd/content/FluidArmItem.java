package dev.ccd.content;

import java.util.ArrayList;
import java.util.List;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint.Mode;
import dev.ccd.logistics.FluidArmPoint;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

/** Server-authoritative preselection through the normal item-use packet, before a container opens. */
public final class FluidArmItem extends BlockItem {
    private static final String KEY = "CCDFluidArmSelection";
    public record Selected(BlockPos pos, Direction face, boolean input) {}
    public FluidArmItem(Block block, Properties properties) { super(block, properties); }
    private static CompoundTag selection(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(KEY);
    }
    private static void save(ItemStack stack, CompoundTag selection) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (selection.isEmpty()) root.remove(KEY);
        else root.put(KEY, selection);
        if (root.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }
    public static List<Selected> selected(ItemStack stack, Level level) {
        CompoundTag selection = selection(stack);
        List<Selected> result = new ArrayList<>();
        if (!selection.getString("Dimension").equals(level.dimension().location().toString())) return result;
        ListTag points = selection.getList("Points", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(points.size(), FluidArmBlockEntity.MAX_POINTS); i++) {
            CompoundTag tag = points.getCompound(i);
            int face = tag.getInt("Face");
            if (face < 0 || face >= Direction.values().length || !tag.contains("Pos", Tag.TAG_COMPOUND)) continue;
            result.add(new Selected(NBTHelper.readBlockPos(tag, "Pos"), Direction.from3DDataValue(face), tag.getBoolean("Input")));
        }
        return result;
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isSpectator()) return InteractionResult.PASS;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.hasChunkAt(pos)) return InteractionResult.PASS;
        if (level.getBlockEntity(pos) instanceof FluidArmBlockEntity arm) {
            if (!level.isClientSide()) {
                if (player.isShiftKeyDown()) {
                    arm.configure(List.of());
                    save(stack, new CompoundTag());
                    player.displayClientMessage(Component.translatable("ccd.fluid_arm.cleared"), true);
                } else apply(stack, level, arm, player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        Direction face = context.getClickedFace();
        if (level.getCapability(Capabilities.FluidHandler.BLOCK, pos, face) == null) return InteractionResult.PASS;
        if (!FluidArmPoint.canTarget(level, pos)) {
            if (!level.isClientSide()) player.displayClientMessage(Component.translatable("ccd.fluid_arm.hatch_required").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        List<Selected> points = new ArrayList<>(selected(stack, level));
        int index = -1;
        for (int i = 0; i < points.size(); i++) if (points.get(i).pos().equals(pos) && points.get(i).face() == face) { index = i; break; }
        String action;
        if (index >= 0 && (player.isShiftKeyDown() || !points.get(index).input())) {
            points.remove(index); action = "removed";
        } else if (index >= 0) {
            points.set(index, new Selected(pos.immutable(), face, false)); action = "output";
        } else if (player.isShiftKeyDown()) {
            return InteractionResult.SUCCESS;
        } else {
            if (points.size() >= FluidArmBlockEntity.MAX_POINTS) {
                player.displayClientMessage(Component.translatable("ccd.fluid_arm.limit", FluidArmBlockEntity.MAX_POINTS).withStyle(ChatFormatting.RED), true);
                return InteractionResult.SUCCESS;
            }
            points.add(new Selected(pos.immutable(), face, true)); action = "input";
        }
        CompoundTag selection = new CompoundTag();
        selection.putString("Dimension", level.dimension().location().toString());
        ListTag tags = new ListTag();
        for (Selected point : points) {
            CompoundTag tag = new CompoundTag();
            tag.put("Pos", NbtUtils.writeBlockPos(point.pos()));
            tag.putInt("Face", point.face().get3DDataValue());
            tag.putBoolean("Input", point.input());
            tags.add(tag);
        }
        selection.put("Points", tags);
        save(stack, selection);
        player.getInventory().setChanged();
        player.displayClientMessage(Component.translatable("ccd.fluid_arm." + action).withStyle(
            action.equals("input") ? ChatFormatting.AQUA : action.equals("output") ? ChatFormatting.GOLD : ChatFormatting.GRAY), true);
        return InteractionResult.SUCCESS;
    }
    private boolean apply(ItemStack stack, Level level, FluidArmBlockEntity arm, Player player) {
        List<Selected> selected = selected(stack, level);
        if (selected.isEmpty()) {
            player.displayClientMessage(Component.translatable("ccd.fluid_arm.no_selection").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        List<FluidArmPoint> points = new ArrayList<>();
        for (Selected point : selected) {
            if (point.pos().equals(arm.getBlockPos()) || !point.pos().closerThan(arm.getBlockPos(), FluidArmBlockEntity.getRange())
                || !level.hasChunkAt(point.pos())) continue;
            var target = new FluidArmPoint(level, point.pos(), level.getBlockState(point.pos()), point.face(),
                point.input() ? Mode.TAKE : Mode.DEPOSIT);
            if (target.isValid()) points.add(target);
        }
        if (points.isEmpty()) {
            player.displayClientMessage(Component.translatable("ccd.fluid_arm.no_valid_points").withStyle(ChatFormatting.RED), true);
            return false;
        }
        arm.configure(points);
        save(stack, new CompoundTag());
        player.getInventory().setChanged();
        long inputs = points.stream().filter(point -> point.getMode() == Mode.TAKE).count();
        player.displayClientMessage(Component.translatable("ccd.fluid_arm.configured", inputs,
            points.size() - inputs, selected.size() - points.size()), true);
        return true;
    }
    @Override protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, Player player,
        ItemStack stack, BlockState state) {
        boolean applied = super.updateCustomBlockEntityTag(pos, level, player, stack, state);
        if (!level.isClientSide() && player != null && !selected(stack, level).isEmpty()
            && level.getBlockEntity(pos) instanceof FluidArmBlockEntity arm) applied |= apply(stack, level, arm, player);
        return applied;
    }
}
