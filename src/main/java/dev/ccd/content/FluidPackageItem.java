package dev.ccd.content;

import java.util.List;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.box.PackageStyles;
import dev.ccd.CopperDynamics;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

public final class FluidPackageItem extends PackageItem {
    public static final int CAPACITY = 4_000;

    public FluidPackageItem(Properties properties) {
        // Reuse a native rigging size; the model is CCD's own resource.
        super(properties, new PackageStyles.PackageStyle("cardboard", 12, 12, 23F, false));
        // The superclass registers every package as a random ordinary box style.
        // Remove only our own instance. Native recognition uses instanceof, while
        // ALL_BOXES also supplies the default ordinary package used by Create.
        PackageStyles.ALL_BOXES.remove(this);
        PackageStyles.STANDARD_BOXES.remove(this);
        PackageStyles.RARE_BOXES.remove(this);
    }

    @Override public String getDescriptionId() { return "item.ccd.fluid_package"; }
    @Override public boolean hasCustomEntity(ItemStack stack) {
        return !stack.getOrDefault(CopperDynamics.RECOVERED.get(), false);
    }
    public static boolean isFluidPackage(ItemStack stack) { return stack.getItem() instanceof FluidPackageItem; }
    public static FluidStack contents(ItemStack stack) {
        if (!isFluidPackage(stack)) return FluidStack.EMPTY;
        FluidCargo cargo = stack.get(CopperDynamics.FLUID_CARGO.get());
        return cargo == null ? FluidStack.EMPTY : cargo.fluid();
    }
    public static boolean isValid(ItemStack stack) {
        return isFluidPackage(stack) && stack.getCount() == 1 && !contents(stack).isEmpty()
            && stack.getOrDefault(AllDataComponents.PACKAGE_CONTENTS,
                net.minecraft.world.item.component.ItemContainerContents.EMPTY).stream().allMatch(ItemStack::isEmpty);
    }
    /** No capacity check here: saved packages must remain recoverable after configuration changes. */
    public static ItemStack containingFluid(FluidStack fluid) {
        if (fluid.isEmpty()) throw new IllegalArgumentException("Cannot package empty fluid");
        ItemStack box = new ItemStack(CopperDynamics.FLUID_PACKAGE.get());
        box.set(CopperDynamics.FLUID_CARGO.get(), new FluidCargo(fluid));
        return box;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flags) {
        FluidStack fluid = contents(stack);
        if (fluid.isEmpty()) {
            lines.add(Component.translatable("ccd.package.invalid").withStyle(ChatFormatting.RED));
        } else {
            lines.add(fluid.getHoverName().copy().withStyle(ChatFormatting.AQUA));
            lines.add(Component.translatable("ccd.package.amount", fluid.getAmount()).withStyle(ChatFormatting.GRAY));
        }
        String address = PackageItem.getAddress(stack);
        if (!address.isBlank()) lines.add(Component.literal("→ " + address).withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("ccd.package.sealed").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> open(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) player.displayClientMessage(Component.translatable("ccd.package.use_packager"), true);
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, player.getItemInHand(hand));
    }
}
