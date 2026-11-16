package dev.ccd.client;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelScreen;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsPacket;
import dev.ccd.content.FluidGaugeBehaviour;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

/** Native panel configuration screen, with an exact mB entry using its existing
 * ValueSettingsPacket rather than a second configuration protocol. */
public final class FluidGaugeScreen extends FactoryPanelScreen {
    private final FluidGaugeBehaviour panel;
    private EditBox target;
    public FluidGaugeScreen(FluidGaugeBehaviour panel) { super(panel); this.panel = panel; }
    @Override protected void init() {
        super.init();
        target = new EditBox(font, guiLeft + 58, guiTop - 42, 88, 18, Component.translatable("ccd.gauge.target_amount"));
        target.setMaxLength(6);
        target.setFilter(value -> value.isEmpty() || value.matches("[0-9]{1,6}") && Integer.parseInt(value) <= 100_000);
        target.setValue(Integer.toString(panel.count));
        addRenderableWidget(target);
        addRenderableWidget(Button.builder(Component.translatable("ccd.gauge.apply"), button -> {
            if (target.getValue().isEmpty()) return;
            int value = Integer.parseInt(target.getValue());
            CatnipServices.NETWORK.sendToServer(new ValueSettingsPacket(panel.getPos(), 0, value,
                null, null, Direction.UP, false, panel.netId()));
        }).bounds(guiLeft + 150, guiTop - 42, 42, 18).build());
    }
    @Override protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderWindow(graphics, mouseX, mouseY, partialTicks);
        graphics.drawString(font, Component.translatable("ccd.gauge.exact_mb"), guiLeft, guiTop - 37, 0xEEEEEE, false);
    }
}
