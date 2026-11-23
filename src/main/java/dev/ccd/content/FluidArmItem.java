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
import com.simibubi.create.content.kinetics.mechanicalArm.ArmItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Server-authoritative preselection through the normal item-use packet, before a container opens. */
public final class FluidArmItem extends ArmItem {
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
            if (face < 0 || face >= Direction.values().length || !tag.contains("Pos", Tag.TAG_INT_ARRAY)) continue;
            var position=NbtUtils.readBlockPos(tag,"Pos");
            if(position.isEmpty()) continue;
            result.add(new Selected(position.get(), Direction.from3DDataValue(face), tag.getBoolean("Input")));
        }
        return result;
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if(!dev.ccd.CCDFeatures.enabled("fluid_arm"))return InteractionResult.FAIL;
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
        boolean fillingTarget=FluidArmPoint.fillingTarget(level,pos)!=null;
        if (FluidArmPoint.interactionHandler(level, pos, face) == null && !fillingTarget) return InteractionResult.PASS;
        if (!FluidArmPoint.canTarget(level, pos) && !fillingTarget) {
            if (!level.isClientSide()) player.displayClientMessage(Component.translatable("ccd.fluid_arm.hatch_required").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        List<Selected> points = new ArrayList<>(selected(stack, level));
        int index = -1;
        for (int i = 0; i < points.size(); i++) if (points.get(i).pos().equals(pos) && points.get(i).face() == face) { index = i; break; }
        String action;
        if (index >= 0) {
            boolean input=!fillingTarget&&!points.get(index).input();
            points.set(index, new Selected(pos.immutable(), face, input)); action = input?"input":"output";
        } else {
            if (points.size() >= FluidArmBlockEntity.MAX_POINTS) {
                player.displayClientMessage(Component.translatable("ccd.fluid_arm.limit", FluidArmBlockEntity.MAX_POINTS).withStyle(ChatFormatting.RED), true);
                return InteractionResult.SUCCESS;
            }
            points.add(new Selected(pos.immutable(), face, !fillingTarget)); action = fillingTarget?"output":"input";
        }
        savePoints(stack,level,points);
        player.getInventory().setChanged();
        Mode mode=action.equals("input")?Mode.TAKE:Mode.DEPOSIT;
        com.simibubi.create.foundation.utility.CreateLang.builder()
            .translate(mode.getTranslationKey(),com.simibubi.create.foundation.utility.CreateLang.blockName(level.getBlockState(pos))
                .style(ChatFormatting.WHITE))
            .color(mode.getColor()).sendStatus(player);
        return InteractionResult.SUCCESS;
    }
    private static void savePoints(ItemStack stack,Level level,List<Selected> points) {
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
    }
    public static boolean deselect(ItemStack stack,Level level,BlockPos pos,Player player) {
        var points=new ArrayList<>(selected(stack,level));
        if(!points.removeIf(point->point.pos().equals(pos))) return false;
        if(!level.isClientSide()) {
            savePoints(stack,level,points);
            player.getInventory().setChanged();
            player.displayClientMessage(Component.translatable("ccd.fluid_arm.removed"),true);
        }
        return true;
    }
    @Override public boolean canAttackBlock(BlockState state,Level level,BlockPos pos,Player player) {
        return !selected(player.getMainHandItem(),level).stream().anyMatch(point->point.pos().equals(pos))
            && !(FluidArmPoint.canTarget(level,pos) && java.util.Arrays.stream(Direction.values())
                .anyMatch(face->FluidArmPoint.interactionHandler(level,pos,face)!=null));
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        // onItemUseFirst handles fluid selection; do not consume unrelated
        // item inventories through ArmItem's item-target detection.
        return place(new net.minecraft.world.item.context.BlockPlaceContext(context));
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
        // ArmItem sends an item-only placement request here. Fluid selections
        // already arrived through normal item-use packets, including their faces.
        boolean applied = false;
        if (!level.isClientSide() && player != null && !selected(stack, level).isEmpty()
            && level.getBlockEntity(pos) instanceof FluidArmBlockEntity arm) applied |= apply(stack, level, arm, player);
        return applied;
    }
}
