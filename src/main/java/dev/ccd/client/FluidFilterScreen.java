package dev.ccd.client;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import dev.ccd.content.*;
import dev.ccd.filter.FluidFilterPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Create's filter layout and widgets, with non-consuming fluid ghost samples. */
public final class FluidFilterScreen extends AbstractSimiContainerScreen<FluidFilterMenu> {
    private final CompoundTag settings;
    private FluidStack pending=FluidStack.EMPTY;
    private int choice;
    private boolean draggingSample;
    private IconButton deny,allow,matching,ignore,add,inverted;
    private SelectionScrollInput selector;
    private Label selectorLabel;
    private AllGuiTextures background(){return menu.attributes?AllGuiTextures.ATTRIBUTE_FILTER:AllGuiTextures.FILTER;}
    public FluidFilterScreen(FluidFilterMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title);settings=FluidFilterItem.data(menu.filter);
        imageWidth=background().getWidth();imageHeight=background().getHeight()+4+AllGuiTextures.PLAYER_INVENTORY.getHeight();
    }
    private IconButton button(int x,int y,AllIcons icon,String tip,Runnable callback){
        var button=addRenderableWidget(new IconButton(leftPos+x,topPos+y,icon));
        button.setToolTip(Component.translatable(tip));button.withCallback(callback);return button;
    }
    @Override protected void init(){
        setWindowOffset(-11,menu.attributes?7:5);super.init();
        int row=menu.attributes?61:75;
        deny=button(menu.attributes?74:18,row,menu.attributes?AllIcons.I_WHITELIST_NOT:AllIcons.I_BLACKLIST,"ccd.filter.blacklist",()->edit(2,0,FluidStack.EMPTY,1));
        allow=button(menu.attributes?38:36,row,menu.attributes?AllIcons.I_WHITELIST_OR:AllIcons.I_WHITELIST,menu.attributes?"ccd.filter.any":"ccd.filter.whitelist",()->{edit(2,0,FluidStack.EMPTY,0);if(menu.attributes)edit(3,0,FluidStack.EMPTY,0);});
        matching=button(menu.attributes?56:60,row,menu.attributes?AllIcons.I_WHITELIST_AND:AllIcons.I_RESPECT_NBT,menu.attributes?"ccd.filter.all":"ccd.filter.exact",()->{edit(3,0,FluidStack.EMPTY,1);if(menu.attributes)edit(2,0,FluidStack.EMPTY,0);});
        if(!menu.attributes)ignore=button(78,row,AllIcons.I_IGNORE_NBT,"ccd.filter.type",()->edit(3,0,FluidStack.EMPTY,0));
        else{
            selectorLabel=new Label(leftPos+43,topPos+31,Component.empty()).colored(0xF3EBDE).withShadow();
            selector=addRenderableWidget(new SelectionScrollInput(leftPos+39,topPos+26,137,18));
            addRenderableWidget(selectorLabel);
            add=button(182,26,AllIcons.I_ADD,"create.gui.attribute_filter.add_attribute",()->addAttribute(false));
            inverted=button(200,26,AllIcons.I_ADD_INVERTED_ATTRIBUTE,"create.gui.attribute_filter.add_inverted_attribute",()->addAttribute(true));
            refreshSelector();
        }
        button(imageWidth-62,background().getHeight()-24,AllIcons.I_TRASH,"ccd.filter.clear",()->edit(5,0,FluidStack.EMPTY,0));
        button(imageWidth-33,background().getHeight()-24,AllIcons.I_CONFIRM,"gui.done",()->minecraft.player.closeContainer());
        indicators();
    }
    private net.minecraft.nbt.ListTag entries(){return settings.getList("entries",Tag.TAG_COMPOUND);}
    private Component describe(CompoundTag entry){
        String kind=entry.getString("kind"),value=entry.getString("value");
        Object label=value;
        if(java.util.Set.of("temperature","density","light").contains(kind))label=Component.translatable("ccd.filter.value."+value);
        else if(kind.equals("mod"))label=net.neoforged.fml.ModList.get().getModContainerById(value)
            .map(container->container.getModInfo().getDisplayName()).orElse(value);
        return Component.translatable("ccd.filter.attribute."+kind,label);
    }
    private CompoundTag entryAt(int slot){
        var entries=entries();for(int i=0;i<entries.size();i++)if(FluidFilterEditor.position(entries.getCompound(i),i)==slot)return entries.getCompound(i);
        return new CompoundTag();
    }
    private void edit(int operation,int slot,FluidStack fluid,int value){
        if(!FluidFilterEditor.apply(settings,menu.attributes,minecraft.level.registryAccess(),operation,slot,fluid,value))return;
        PacketDistributor.sendToServer(new FluidFilterPacket(menu.containerId,operation,slot,fluid,value));indicators();
    }
    private void indicators(){
        if(deny==null)return;
        boolean blacklist=settings.getBoolean("blacklist"),all=settings.getBoolean(menu.attributes?"all":"components");
        deny.green=blacklist;allow.green=!blacklist&&(!menu.attributes||!all);matching.green=all&&(!menu.attributes||!blacklist);
        if(ignore!=null)ignore.green=!all;
        if(add!=null){
            var choices=FluidFilterRules.choices(pending);boolean enabled=!choices.isEmpty()&&entries().size()<FluidFilterRules.SLOTS;
            if(enabled){var selected=choices.get(Math.min(choice,choices.size()-1));for(int i=0;i<entries().size();i++){var e=entries().getCompound(i);if(e.getString("kind").equals(selected.getString("kind"))&&e.getString("value").equals(selected.getString("value")))enabled=false;}}
            add.active=inverted.active=enabled;
        }
    }
    private void refreshSelector(){
        if(selector==null)return;
        var choices=FluidFilterRules.choices(pending);selector.removeCallback();
        selector.forOptions(choices.isEmpty()?java.util.List.of(Component.empty()):choices.stream().map(this::describe).toList());
        choice=Math.min(choice,Math.max(0,choices.size()-1));selector.setState(choice);selector.visible=selector.active=!choices.isEmpty();
        if(choices.isEmpty())selectorLabel.setTextAndTrim(Component.translatable("ccd.filter.choose_sample"),true,128);
        else{
            selector.titled(pending.getHoverName().copy());
            selector.calling(value->{choice=value;selectorLabel.setTextAndTrim(describe(choices.get(value)),true,128);indicators();});
            selector.onChanged();
        }
        indicators();
    }
    private void sample(FluidStack fluid){pending=fluid.isEmpty()?FluidStack.EMPTY:fluid.copyWithAmount(1);choice=0;refreshSelector();}
    private void addAttribute(boolean invert){if(!pending.isEmpty())edit(invert?6:0,entries().size(),pending,choice);}
    public int targetCount(){return menu.attributes?1:FluidFilterRules.SLOTS;}
    public Rect2i target(int slot){
        if(menu.attributes)return new Rect2i(leftPos+15,topPos+26,18,18);
        return new Rect2i(leftPos+22+(slot%9)*18,topPos+24+(slot/9)*18,18,18);
    }
    public void acceptFluid(int slot,FluidStack fluid){if(fluid.isEmpty())return;if(menu.attributes)sample(fluid);else edit(0,slot,fluid.copyWithAmount(1),0);}
    private int inventoryLeft(){return getLeftOfCentered(176);}
    private int inventoryTop(){return topPos+background().getHeight()+4;}
    private int inventoryX(int slot){return inventoryLeft()+8+(slot%9)*18;}
    private int inventoryY(int slot){return inventoryTop()+(slot<9?76:18+((slot-9)/9)*18);}
    @Override public boolean mouseClicked(double x,double y,int button){
        // Give native widgets first refusal; sample selection never overlaps their hit boxes.
        for(var child:children())if(child.isMouseOver(x,y)&&child.mouseClicked(x,y,button)){setFocused(child);return true;}
        for(int i=0;i<targetCount();i++)if(target(i).contains((int)x,(int)y)){
            if(button==1){if(menu.attributes)sample(FluidStack.EMPTY);else edit(1,i,FluidStack.EMPTY,0);}
            else if(button==0&&!pending.isEmpty()&&!menu.attributes)acceptFluid(i,pending);
            return true;
        }
        if(menu.attributes&&new Rect2i(leftPos+15,topPos+61,18,18).contains((int)x,(int)y))return true;
        for(int i=0;i<36;i++)if(x>=inventoryX(i)-1&&x<inventoryX(i)+17&&y>=inventoryY(i)-1&&y<inventoryY(i)+17){
            if(button!=0)return true;
            var fluid=FluidStockItem.sample(minecraft.player.getInventory().getItem(i));sample(fluid);draggingSample=!fluid.isEmpty();
            if(hasShiftDown()&&!menu.attributes&&!fluid.isEmpty())for(int slot=0;slot<FluidFilterRules.SLOTS;slot++)if(entryAt(slot).isEmpty()){acceptFluid(slot,fluid);break;}
            return true;
        }
        return false;
    }
    @Override public boolean mouseReleased(double x,double y,int button){
        boolean dragged=draggingSample;draggingSample=false;
        if(dragged&&button==0)for(int i=0;i<targetCount();i++)if(target(i).contains((int)x,(int)y)){acceptFluid(i,pending);return true;}
        return super.mouseReleased(x,y,button);
    }
    @Override protected void renderBg(GuiGraphics g,float pt,int mx,int my){
        renderPlayerInventory(g,inventoryLeft(),inventoryTop());background().render(g,leftPos,topPos);
        g.drawString(font,title,leftPos+(imageWidth-8-font.width(title))/2,topPos+4,menu.attributes?0x592424:0x303030,false);
        if(menu.attributes){drawFluid(g,pending,leftPos+16,topPos+27);g.renderItem(menu.filter,leftPos+16,topPos+62);g.renderItemDecorations(font,menu.filter,leftPos+16,topPos+62,Integer.toString(entries().size()));}
        else for(int slot=0;slot<FluidFilterRules.SLOTS;slot++){var r=target(slot);drawFluid(g,FluidStack.parseOptional(minecraft.level.registryAccess(),entryAt(slot).getCompound("fluid")),r.getX()+1,r.getY()+1);}
        for(int i=0;i<36;i++){var item=minecraft.player.getInventory().getItem(i);g.renderItem(item,inventoryX(i),inventoryY(i));g.renderItemDecorations(font,item,inventoryX(i),inventoryY(i));}
    }
    private void drawFluid(GuiGraphics g,FluidStack fluid,int x,int y){
        if(fluid.isEmpty())return;var extension=IClientFluidTypeExtensions.of(fluid.getFluid());
        var sprite=minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(extension.getStillTexture(fluid));int color=extension.getTintColor(fluid);
        g.setColor(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,1);g.blit(x,y,0,16,16,sprite);g.setColor(1,1,1,1);
    }
    @Override protected void renderForeground(GuiGraphics g,int x,int y,float pt){
        super.renderForeground(g,x,y,pt);
        if(menu.attributes&&new Rect2i(leftPos+15,topPos+61,18,18).contains(x,y)){
            var lines=new java.util.ArrayList<Component>();lines.add(Component.translatable("ccd.filter.attributes"));
            for(int i=0;i<entries().size();i++){var e=entries().getCompound(i);lines.add(Component.literal(e.getBoolean("invert")?"! ":"- ").append(describe(e)));}
            g.renderComponentTooltip(font,lines,x,y);
        }
        for(int i=0;i<targetCount();i++)if(target(i).contains(x,y)){
            var fluid=menu.attributes?pending:FluidStack.parseOptional(minecraft.level.registryAccess(),entryAt(i).getCompound("fluid"));
            if(!fluid.isEmpty())g.renderTooltip(font,fluid.getHoverName(),x,y);
        }
        for(int i=0;i<36;i++)if(x>=inventoryX(i)&&x<inventoryX(i)+16&&y>=inventoryY(i)&&y<inventoryY(i)+16){var item=minecraft.player.getInventory().getItem(i);if(!item.isEmpty())g.renderTooltip(font,item,x,y);}
        if(draggingSample&&!pending.isEmpty())drawFluid(g,pending,x+8,y+8);
    }
}
