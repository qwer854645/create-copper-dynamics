package dev.ccd.client;

import dev.ccd.content.FluidFilterMenu;
import dev.ccd.content.FluidFilterItem;
import dev.ccd.content.FluidFilterRules;
import dev.ccd.content.FluidStockItem;
import dev.ccd.filter.FluidFilterPacket;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
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
    private IconButton mode, matching;
    private IconButton allow, ignore;
    private Button attribute;
    private AllGuiTextures background() { return menu.attributes ? AllGuiTextures.ATTRIBUTE_FILTER : AllGuiTextures.FILTER; }
    public FluidFilterScreen(FluidFilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = background().getWidth(); imageHeight = background().getHeight() + 4 + AllGuiTextures.PLAYER_INVENTORY.getHeight();
        settings = FluidFilterItem.data(menu.filter);
    }
    @Override protected void init() {
        super.init();
        int row = menu.attributes ? 61 : 75;
        mode = addRenderableWidget(new IconButton(leftPos + (menu.attributes ? 74 : 18), topPos + row, AllIcons.I_BLACKLIST));
        mode.withCallback(() -> { if(!settings.getBoolean("blacklist")){settings.putBoolean("blacklist",true);send(2,0,FluidStack.EMPTY,0);}labels(); });
        matching = addRenderableWidget(new IconButton(leftPos + (menu.attributes ? 56 : 60), topPos + row, menu.attributes ? AllIcons.I_WHITELIST_AND : AllIcons.I_RESPECT_NBT));
        matching.withCallback(() -> { String key = menu.attributes ? "all" : "components"; if(!settings.getBoolean(key)){settings.putBoolean(key,true);send(3,0,FluidStack.EMPTY,0);}if(menu.attributes&&settings.getBoolean("blacklist")){settings.putBoolean("blacklist",false);send(2,0,FluidStack.EMPTY,0);}labels(); });
        allow=addRenderableWidget(new IconButton(leftPos+(menu.attributes?38:36),topPos+row,menu.attributes?AllIcons.I_WHITELIST_OR:AllIcons.I_WHITELIST));
        allow.withCallback(() -> {if(settings.getBoolean("blacklist")){settings.putBoolean("blacklist",false);send(2,0,FluidStack.EMPTY,0);}if(menu.attributes&&settings.getBoolean("all")){settings.putBoolean("all",false);send(3,0,FluidStack.EMPTY,0);}labels();});
        allow.setToolTip(Component.translatable("ccd.filter.whitelist"));
        if(!menu.attributes){
            ignore=addRenderableWidget(new IconButton(leftPos+78,topPos+row,AllIcons.I_IGNORE_NBT));
            ignore.withCallback(() -> {if(settings.getBoolean("components")){settings.putBoolean("components",false);send(3,0,FluidStack.EMPTY,0);}labels();});
            ignore.setToolTip(Component.translatable("ccd.filter.type"));
        }
        if (menu.attributes) attribute = addRenderableWidget(Button.builder(Component.empty(), b -> {
            var choices = FluidFilterRules.choices(pending); if (!choices.isEmpty()) choice = (choice + 1) % choices.size(); labels();
        }).bounds(leftPos + 39, topPos + 26, 137, 18).build());
        if(menu.attributes) {
            var add=addRenderableWidget(new IconButton(leftPos+182,topPos+26,AllIcons.I_ADD));
            add.withCallback(() -> acceptFluid(settings.getList("entries",Tag.TAG_COMPOUND).size(),pending));
            var inverted=addRenderableWidget(new IconButton(leftPos+200,topPos+26,AllIcons.I_ADD_INVERTED_ATTRIBUTE));
            inverted.withCallback(() -> {
                int index=settings.getList("entries",Tag.TAG_COMPOUND).size();
                if(pending.isEmpty() || index>=FluidFilterRules.SLOTS)return;
                acceptFluid(index,pending);
                settings.getList("entries",Tag.TAG_COMPOUND).getCompound(index).putBoolean("invert",true);
                send(4,index,FluidStack.EMPTY,0);
            });
        }
        var clear=addRenderableWidget(new IconButton(leftPos+imageWidth-62,topPos+background().getHeight()-24,AllIcons.I_TRASH));
        clear.withCallback(() -> { settings.put("entries",new net.minecraft.nbt.ListTag());send(5,0,FluidStack.EMPTY,0); });
        var confirm=addRenderableWidget(new IconButton(leftPos+imageWidth-33,topPos+background().getHeight()-24,AllIcons.I_CONFIRM));
        confirm.withCallback(() -> minecraft.player.closeContainer());
        labels();
    }
    private void labels() {
        mode.setToolTip(Component.translatable("ccd.filter.blacklist"));
        mode.green = settings.getBoolean("blacklist");
        matching.setToolTip(Component.translatable(menu.attributes ? "ccd.filter.all" : "ccd.filter.exact"));
        matching.green = settings.getBoolean(menu.attributes ? "all" : "components") && (!menu.attributes || !settings.getBoolean("blacklist"));
        allow.green = !settings.getBoolean("blacklist") && (!menu.attributes || !settings.getBoolean("all"));
        if(ignore!=null)ignore.green=!settings.getBoolean("components");
        if (attribute != null) {
            var choices = FluidFilterRules.choices(pending);
            attribute.setMessage(choices.isEmpty() ? Component.translatable("ccd.filter.choose_sample") : describe(choices.get(choice)));
        }
    }
    private static Component describe(CompoundTag entry) {
        return Component.translatable("ccd.filter.attribute." + entry.getString("kind"), entry.getString("value"));
    }
    public net.minecraft.client.renderer.Rect2i target(int slot) {
        if(menu.attributes)return new net.minecraft.client.renderer.Rect2i(leftPos+16,topPos+62,16,16);
        return new net.minecraft.client.renderer.Rect2i(leftPos + 26 + (slot % 9) * 18, topPos + 29 + (slot / 9) * 18, 18, 18);
    }
    public void acceptFluid(int slot, FluidStack fluid) {
        if (fluid.isEmpty()) return;
        if(slot>=FluidFilterRules.SLOTS)return;
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
        if(menu.attributes && x>=leftPos+16 && x<leftPos+34 && y>=topPos+26 && y<topPos+44) {
            if(!pending.isEmpty()){choice=0;labels();}return true;
        }
        if(menu.attributes && target(0).contains((int)x,(int)y)) {
            if(button==1 && !entries.isEmpty()){int last=entries.size()-1;entries.remove(last);settings.put("entries",entries);send(1,last,FluidStack.EMPTY,0);}return true;
        }
        for (int i = 0; !menu.attributes && i < FluidFilterRules.SLOTS; i++) if (target(i).contains((int)x, (int)y)) {
            if (button == 1 && i < entries.size()) { entries.remove(i); settings.put("entries", entries); send(1, i, FluidStack.EMPTY, 0); }
            else if (button == 0 && hasShiftDown() && menu.attributes && i < entries.size()) {
                var e = entries.getCompound(i); e.putBoolean("invert", !e.getBoolean("invert")); settings.put("entries", entries); send(4, i, FluidStack.EMPTY, 0);
            } else if (button == 0 && !pending.isEmpty()) acceptFluid(i, pending);
            return true;
        }
        // Select a sample from the displayed player inventory without picking it up.
        for (int i = 0; i < 36; i++) {
            int px = inventoryX(i), py = inventoryY(i);
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
        if(dragged && menu.attributes && !pending.isEmpty()){labels();return true;}
        if (dragged && !pending.isEmpty()) for (int i = 0; i < FluidFilterRules.SLOTS; i++) if (target(i).contains((int)x, (int)y)) { acceptFluid(i, pending); return true; }
        return super.mouseReleased(x, y, button);
    }
    @Override protected void renderBg(GuiGraphics g, float pt, int mouseX, int mouseY) {
        background().render(g,leftPos,topPos);
        AllGuiTextures.PLAYER_INVENTORY.render(g,leftPos+(imageWidth-176)/2,topPos+background().getHeight()+4);
        var entries = settings.getList("entries", Tag.TAG_COMPOUND);
        if(menu.attributes){
            drawFluid(g,pending,leftPos+16,topPos+27);
            g.renderItem(menu.filter,leftPos+16,topPos+62);
            g.drawString(font,Integer.toString(entries.size()),leftPos+27,topPos+71,0xffffff,true);
        }
        for (int i = 0; !menu.attributes && i < FluidFilterRules.SLOTS; i++) {
            var r = target(i);
            if (i < entries.size()) drawFluid(g, FluidStack.parseOptional(minecraft.level.registryAccess(), entries.getCompound(i).getCompound("fluid")), r.getX()+1, r.getY()+1);
        }
        for (int i = 0; i < 36; i++) {
            int x = inventoryX(i), y = inventoryY(i);
            g.renderItem(minecraft.player.getInventory().getItem(i), x+1, y+1);
        }
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
    private int inventoryX(int slot){return leftPos+(imageWidth-176)/2+7+(slot%9)*18;}
    private int inventoryY(int slot){return topPos+background().getHeight()+4+(slot<9?75:17+((slot-9)/9)*18);}
    @Override protected void renderLabels(GuiGraphics g, int x, int y) { g.drawString(font, title, (imageWidth-8-font.width(title))/2, 4, 0xff38332c, false); }
    @Override public void render(GuiGraphics g, int x, int y, float pt) {
        super.render(g, x, y, pt);
        var entries = settings.getList("entries", Tag.TAG_COMPOUND);
        if(menu.attributes && target(0).contains(x,y)) {
            var lines=new java.util.ArrayList<Component>();
            for(int i=0;i<entries.size();i++){var e=entries.getCompound(i);lines.add(Component.literal(e.getBoolean("invert")?"! ":"- ").append(describe(e)));}
            lines.add(Component.translatable("ccd.filter.attribute_help"));
            g.renderTooltip(font,lines,java.util.Optional.empty(),x,y);
        }
        for (int i=0;!menu.attributes && i<entries.size();i++) if(target(i).contains(x,y)) {
            var entry=entries.getCompound(i);
            var fluid=FluidStack.parseOptional(minecraft.level.registryAccess(),entry.getCompound("fluid"));
            if(menu.attributes) g.renderTooltip(font,List.of(describe(entry),Component.translatable(entry.getBoolean("invert")?"ccd.filter.inverted":"ccd.filter.attribute_help")),java.util.Optional.empty(),x,y);
            else if(!fluid.isEmpty()) g.renderTooltip(font,fluid.getHoverName(),x,y);
        }
        if (!pending.isEmpty()) drawFluid(g, pending, x+8, y+8);
    }
}
