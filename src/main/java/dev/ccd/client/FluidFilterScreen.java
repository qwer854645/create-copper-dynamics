package dev.ccd.client;

import dev.ccd.content.FluidFilterMenu;
import dev.ccd.content.FluidFilterItem;
import dev.ccd.content.FluidFilterRules;
import dev.ccd.content.FluidStockItem;
import dev.ccd.filter.FluidFilterPacket;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Ghost samples: inventory clicks and browser drags never move real items. */
public final class FluidFilterScreen extends AbstractContainerScreen<FluidFilterMenu> {
    private CompoundTag settings;
    private FluidStack pending = FluidStack.EMPTY;
    private int choice;
    private boolean draggingSample;
    private Button mode, matching, attribute;
    public FluidFilterScreen(FluidFilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 244; imageHeight = 230;
        settings = FluidFilterItem.data(menu.filter);
    }
    @Override protected void init() {
        super.init();
        mode = addRenderableWidget(Button.builder(Component.empty(), b -> { settings.putBoolean("blacklist", !settings.getBoolean("blacklist")); send(2, 0, FluidStack.EMPTY, 0); labels(); })
            .bounds(leftPos + 8, topPos + 22, 110, 20).build());
        matching = addRenderableWidget(Button.builder(Component.empty(), b -> { String key = menu.attributes ? "all" : "components"; settings.putBoolean(key, !settings.getBoolean(key)); send(3, 0, FluidStack.EMPTY, 0); labels(); })
            .bounds(leftPos + 124, topPos + 22, 112, 20).build());
        if (menu.attributes) attribute = addRenderableWidget(Button.builder(Component.empty(), b -> {
            var choices = FluidFilterRules.choices(pending); if (!choices.isEmpty()) choice = (choice + 1) % choices.size(); labels();
        }).bounds(leftPos + 8, topPos + 92, 228, 20).build());
        labels();
    }
    private void labels() {
        mode.setMessage(Component.translatable(settings.getBoolean("blacklist") ? "ccd.filter.blacklist" : "ccd.filter.whitelist"));
        matching.setMessage(Component.translatable(menu.attributes
            ? (settings.getBoolean("all") ? "ccd.filter.all" : "ccd.filter.any")
            : (settings.getBoolean("components") ? "ccd.filter.exact" : "ccd.filter.type")));
        if (attribute != null) {
            var choices = FluidFilterRules.choices(pending);
            attribute.setMessage(choices.isEmpty() ? Component.translatable("ccd.filter.choose_sample") : describe(choices.get(choice)));
        }
    }
    private static Component describe(CompoundTag entry) {
        return Component.translatable("ccd.filter.attribute." + entry.getString("kind"), entry.getString("value"));
    }
    public net.minecraft.client.renderer.Rect2i target(int slot) {
        return new net.minecraft.client.renderer.Rect2i(leftPos + 41 + (slot % 9) * 18, topPos + 48 + (slot / 9) * 18, 18, 18);
    }
    public void acceptFluid(int slot, FluidStack fluid) {
        if (fluid.isEmpty()) return;
        if (menu.attributes && !FluidStack.isSameFluidSameComponents(pending, fluid)) { pending = fluid.copyWithAmount(1); choice = 0; }
        var entries = settings.getList("entries", Tag.TAG_COMPOUND).copy();
        var entry = menu.attributes ? FluidFilterRules.choices(fluid).get(choice).copy() : new CompoundTag();
        entry.put("fluid", fluid.copyWithAmount(1).saveOptional(minecraft.level.registryAccess()));
        if (slot < entries.size()) entries.set(slot, entry); else if (entries.size() < FluidFilterRules.SLOTS) entries.add(entry);
        settings.put("entries", entries);
        send(0, slot, fluid.copyWithAmount(1), choice); labels();
    }
    private void send(int operation, int slot, FluidStack fluid, int choice) {
        PacketDistributor.sendToServer(new FluidFilterPacket(menu.containerId, operation, slot, fluid, choice));
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        var entries = settings.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < FluidFilterRules.SLOTS; i++) if (target(i).contains((int)x, (int)y)) {
            if (button == 1 && i < entries.size()) { entries.remove(i); settings.put("entries", entries); send(1, i, FluidStack.EMPTY, 0); }
            else if (button == 0 && hasShiftDown() && menu.attributes && i < entries.size()) {
                var e = entries.getCompound(i); e.putBoolean("invert", !e.getBoolean("invert")); settings.put("entries", entries); send(4, i, FluidStack.EMPTY, 0);
            } else if (button == 0 && !pending.isEmpty()) acceptFluid(i, pending);
            return true;
        }
        // Select a sample from the displayed player inventory without picking it up.
        for (int i = 0; i < 36; i++) {
            int px = leftPos + 41 + (i % 9) * 18, py = topPos + 134 + (i / 9) * 18;
            if (x >= px && x < px + 18 && y >= py && y < py + 18) {
                pending = FluidStockItem.sample(minecraft.player.getInventory().getItem(i)); choice = 0;
                draggingSample = !pending.isEmpty(); labels(); return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        boolean dragged = draggingSample;
        draggingSample = false;
        if (dragged && !pending.isEmpty()) for (int i = 0; i < FluidFilterRules.SLOTS; i++) if (target(i).contains((int)x, (int)y)) { acceptFluid(i, pending); return true; }
        return super.mouseReleased(x, y, button);
    }
    @Override protected void renderBg(GuiGraphics g, float pt, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff393b3b);
        g.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xffc4b8a4);
        var entries = settings.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < FluidFilterRules.SLOTS; i++) {
            var r = target(i); g.fill(r.getX(), r.getY(), r.getX()+17, r.getY()+17, 0xff59544b);
            if (i < entries.size()) drawFluid(g, FluidStack.parseOptional(minecraft.level.registryAccess(), entries.getCompound(i).getCompound("fluid")), r.getX()+1, r.getY()+1);
        }
        for (int i = 0; i < 36; i++) {
            int x = leftPos + 41 + (i % 9) * 18, y = topPos + 134 + (i / 9) * 18;
            g.fill(x, y, x+17, y+17, 0xff817769); g.renderItem(minecraft.player.getInventory().getItem(i), x+1, y+1);
        }
        g.drawString(font, Component.translatable("ccd.filter.help"), leftPos+8, topPos+imageHeight-14, 0xff38332c, false);
    }
    private void drawFluid(GuiGraphics g, FluidStack fluid, int x, int y) {
        if (fluid.isEmpty()) return;
        var extension = IClientFluidTypeExtensions.of(fluid.getFluid());
        var sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(extension.getStillTexture(fluid));
        int color = extension.getTintColor(fluid);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(((color>>16)&255)/255f, ((color>>8)&255)/255f, (color&255)/255f, 1);
        g.blit(x, y, 0, 16, 16, sprite);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1, 1, 1, 1);
    }
    @Override protected void renderLabels(GuiGraphics g, int x, int y) { g.drawString(font, title, 8, 8, 0xff38332c, false); }
    @Override public void render(GuiGraphics g, int x, int y, float pt) {
        super.render(g, x, y, pt);
        var entries = settings.getList("entries", Tag.TAG_COMPOUND);
        for (int i=0;i<entries.size();i++) if(target(i).contains(x,y)) {
            var entry=entries.getCompound(i);
            var fluid=FluidStack.parseOptional(minecraft.level.registryAccess(),entry.getCompound("fluid"));
            if(menu.attributes) g.renderTooltip(font,List.of(describe(entry),Component.translatable(entry.getBoolean("invert")?"ccd.filter.inverted":"ccd.filter.attribute_help")),java.util.Optional.empty(),x,y);
            else if(!fluid.isEmpty()) g.renderTooltip(font,fluid.getHoverName(),x,y);
        }
        if (!pending.isEmpty()) drawFluid(g, pending, x+8, y+8);
    }
}
