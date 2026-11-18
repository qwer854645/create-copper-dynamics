package dev.ccd.content;

import java.util.ArrayList;
import java.util.List;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.foundation.item.ItemHelper;
import dev.ccd.CopperDynamics;
import dev.ccd.CCDTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/** A native package with normal item slots plus a separate, direct fluid payload. */
public final class CompositePackageItem extends PackageItem {
    public static final int FLUID_CAPACITY = 8_000;
    public static final int MAX_FLUID_TYPES = 8;
    public CompositePackageItem(Properties properties) {
        super(properties.stacksTo(1), new PackageStyles.PackageStyle("cardboard", 12, 12, 23F, false));
        PackageStyles.ALL_BOXES.remove(this);
        PackageStyles.STANDARD_BOXES.remove(this);
        PackageStyles.RARE_BOXES.remove(this);
    }
    @Override public String getDescriptionId() { return "item.ccd.composite_package"; }
    @Override public boolean hasCustomEntity(ItemStack stack) { return true; }
    @Override public net.minecraft.world.entity.Entity createEntity(Level level, net.minecraft.world.entity.Entity original, ItemStack stack) {
        return original.getPersistentData().getBoolean("CCDRecoveryDrop") ? null : super.createEntity(level, original, stack);
    }
    public static boolean isComposite(ItemStack stack) { return stack.getItem() instanceof CompositePackageItem; }
    public static boolean isSealed(ItemStack stack) { return isComposite(stack) || FluidPackageItem.isFluidPackage(stack); }
    public static List<FluidStack> fluids(ItemStack stack) {
        if (!isComposite(stack)) return List.of();
        CompositeFluids payload = stack.get(CopperDynamics.COMPOSITE_FLUIDS.get());
        return payload == null ? List.of() : payload.entries().stream().map(FluidCargo::fluid).toList();
    }
    public static boolean isValid(ItemStack stack) {
        if (!isComposite(stack) || stack.getCount() != 1 || !stack.has(CopperDynamics.COMPOSITE_FLUIDS.get())) return false;
        var items = PackageItem.getContents(stack);
        boolean hasItems = false;
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack item = items.getStackInSlot(i);
            if (PackageItem.isPackage(item) || item.getItem() instanceof FluidStockItem) return false;
            if (!item.isEmpty()) hasItems = true;
        }
        long volume = fluids(stack).stream().mapToLong(FluidStack::getAmount).sum();
        return (hasItems || volume > 0) && volume <= FLUID_CAPACITY;
    }
    public static ItemStack containing(ItemStackHandler items, List<FluidStack> fluids) {
        if (items.getSlots() != PackageItem.SLOTS) throw new IllegalArgumentException("Expected native package slots");
        ItemStack box = new ItemStack(CopperDynamics.COMPOSITE_PACKAGE.get());
        box.set(AllDataComponents.PACKAGE_CONTENTS, ItemHelper.containerContentsFromHandler(items));
        box.set(CopperDynamics.COMPOSITE_FLUIDS.get(), new CompositeFluids(fluids.stream().map(FluidCargo::new).toList()));
        if (!isValid(box)) throw new IllegalArgumentException("Invalid or oversized composite contents");
        return box;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        for (FluidStack fluid : fluids(stack)) lines.add(Component.translatable("ccd.composite.fluid_line",
            fluid.getHoverName(), CCDTooltip.amount(fluid.getAmount())).withStyle(ChatFormatting.GRAY));
        if (!isValid(stack)) lines.add(Component.translatable("ccd.composite.invalid").withStyle(ChatFormatting.RED));
    }
    @Override public InteractionResultHolder<ItemStack> open(Level level, Player player, InteractionHand hand) {
        ItemStack box = player.getItemInHand(hand);
        if (!isValid(box)) return InteractionResultHolder.fail(box);
        if (level.isClientSide()) return InteractionResultHolder.success(box);
        // This is an explicit manual opening, like Create's own package opening.
        // Return physical items and sealed fluid parcels; never place fluids into the world.
        List<ItemStack> contents = new ArrayList<>();
        var items = PackageItem.getContents(box);
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack item = items.getStackInSlot(i);
            if (!item.isEmpty()) contents.add(item.copy());
        }
        for (FluidStack fluid : fluids(box)) {
            int remaining = fluid.getAmount();
            while (remaining > 0) {
                int amount = Math.min(FluidPackageItem.CAPACITY, remaining);
                ItemStack parcel = FluidPackageItem.containingFluid(fluid.copyWithAmount(amount));
                parcel.set(CopperDynamics.RECOVERED.get(), true);
                contents.add(parcel);
                remaining -= amount;
            }
        }
        player.setItemInHand(hand, ItemStack.EMPTY);
        for (ItemStack content : contents) player.getInventory().placeItemBackInInventory(content);
        com.simibubi.create.AllSoundEvents.PACKAGE_POP.playOnServer(level, player.blockPosition());
        // The returned stack is written back by the caller: the freed hand slot may
        // already contain one of the unpacked items.
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
