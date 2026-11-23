package dev.ccd.content;

import java.util.List;
import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import dev.ccd.CopperDynamics;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

/** Keeps native ticking, connections, configuration packets, summary queries,
 * RequestPromiseQueue and request distribution. Only units and filter conversion differ. */
public final class FluidGaugeBehaviour extends FactoryPanelBehaviour {
    public FluidGaugeBehaviour(FluidGaugeBlockEntity be, PanelSlot slot) { super(be, slot); }
    @Override public void tick() {
        if(!dev.ccd.CCDFeatures.enabled(blockEntity.getBlockState()))return;
        super.tick();
    }

    @Override public boolean setFilter(ItemStack stack) {
        if (stack.isEmpty()) return super.setFilter(stack);
        var fluid = FluidStockItem.sample(stack);
        return !fluid.isEmpty() && super.setFilter(FluidStockItem.of(fluid));
    }

    @Override public void onShortInteract(Player player, InteractionHand hand, Direction side, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!FluidStockItem.sample(held).isEmpty()) {
            if (Create.LOGISTICS.mayInteract(network, player) && !player.level().isClientSide()) setFilter(held);
            return;
        }
        super.onShortInteract(player, hand, side, hit);
    }

    @Override public ValueSettingsBoard createBoard(Player player, BlockHitResult hit) {
        return new ValueSettingsBoard(Component.translatable("ccd.gauge.target_amount"), 100, 10,
            List.of(Component.literal("mB"), Component.literal("B")), new ValueSettingsFormatter(this::formatValue));
    }
    @Override public void setValueSettings(Player player, ValueSettings settings, boolean ctrlDown) {
        int millibuckets = Math.clamp((long) settings.value() * (settings.row() == 1 ? 1_000 : 1), 0, 100_000);
        super.setValueSettings(player, new ValueSettings(0, millibuckets), ctrlDown);
        upTo = true; // Native item/stack multiplication must never change mB quantities.
    }
    @Override public ValueSettings getValueSettings() {
        // Native ValueSettingsScreen allocates one column per value. Keep its
        // quick selector small; the inherited config screen has an exact mB field.
        return count >= 1_000 ? new ValueSettings(1, Math.min(100, count / 1_000)) : new ValueSettings(0, Math.min(100, count));
    }
    @Override public MutableComponent formatValue(ValueSettings settings) {
        return com.simibubi.create.foundation.utility.CreateLang.number(settings.value())
            .translate(settings.row() == 1 ? "generic.unit.buckets" : "generic.unit.millibuckets").component();
    }
    @Override public MutableComponent getCountLabelForValueBox() {
        return getFilter().isEmpty() ? Component.empty()
            : super.getCountLabelForValueBox().append(" mB");
    }
    @Override public MutableComponent getAmountTip() { return Component.translatable("ccd.gauge.amount_tip"); }
    @Override @OnlyIn(Dist.CLIENT) public void displayScreen(Player player) {
        if (player instanceof net.minecraft.client.player.LocalPlayer)
            net.createmod.catnip.gui.ScreenOpener.open(new dev.ccd.client.FluidGaugeScreen(this));
    }
    @Override public ItemRequirement getRequiredItems() {
        return active ? new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, CopperDynamics.FLUID_GAUGE_ITEM.get()) : ItemRequirement.NONE;
    }
}
