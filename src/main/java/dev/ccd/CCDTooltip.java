package dev.ccd;

import java.util.List;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Native goggle indentation and number formatting, shared by CCD's own information sections. */
public final class CCDTooltip {
    private CCDTooltip() {}
    public static void heading(List<Component> tooltip, String key) {
        if (!tooltip.isEmpty()&&!tooltip.getLast().getString().isEmpty()) tooltip.add(Component.empty());
        CreateLang.builder().add(Component.translatable(key)).forGoggles(tooltip);
    }
    /** Match Create's fluid container title, fluid-name rows and quantity colours.
     * Multi-fluid storage has one shared capacity, shown once rather than per fluid. */
    public static void fluidContents(List<Component> tooltip,
        List<net.neoforged.neoforge.fluids.FluidStack> fluids, long capacity) {
        if (fluids.size() <= 1 && capacity <= Integer.MAX_VALUE) {
            var tank=new net.neoforged.neoforge.fluids.capability.templates.FluidTank((int)capacity);
            if(!fluids.isEmpty())tank.setFluid(fluids.getFirst().copy());
            containerContents(tooltip,false,tank);
            return;
        }
        heading(tooltip, "create.gui.goggles.fluid_container");
        for (var fluid : fluids) {
            CreateLang.fluidName(fluid).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            CreateLang.builder().add(amount(fluid.getAmount())).forGoggles(tooltip, 1);
        }
        CreateLang.translate("gui.goggles.fluid_container.capacity")
            .add(amount(capacity)).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
    }
    /** Preserve native per-tank formatting, except for CCD's shared-capacity tanks. */
    public static boolean containerContents(List<Component> tooltip, boolean sneaking,
        net.neoforged.neoforge.fluids.capability.IFluidHandler handler) {
        if (handler == null || handler.getTanks() == 0) return false;
        if (handler instanceof dev.ccd.content.SmartTankClusterInventory smart) {
            var view = smart.snapshot();
            if (view == null) return false;
            fluidContents(tooltip, view.fluids(), view.capacity());
            return true;
        }
        var nativeLines=new java.util.ArrayList<Component>();
        boolean shown=new com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation() {}
            .containedFluidTooltip(nativeLines, sneaking, handler);
        if(!nativeLines.isEmpty()){
            if(!tooltip.isEmpty()&&!tooltip.getLast().getString().isEmpty())tooltip.add(Component.empty());
            tooltip.addAll(nativeLines);
        }
        return shown;
    }
    public static void line(List<Component> tooltip, String key, Object... args) {
        line(tooltip, ChatFormatting.GRAY, key, args);
    }
    /** Like native kinetic statistics: label at the section level, value indented once. */
    public static void stat(List<Component> tooltip,String label,Component value){
        CreateLang.builder().add(Component.translatable(label)).style(ChatFormatting.GRAY).forGoggles(tooltip);
        CreateLang.builder().add(value).forGoggles(tooltip,1);
    }
    public static void line(List<Component> tooltip, ChatFormatting color, String key, Object... args) {
        CreateLang.builder().add(Component.translatable(key, args)).style(color).forGoggles(tooltip, 1);
    }
    public static MutableComponent number(Number value) {
        return CreateLang.number(value.doubleValue()).style(ChatFormatting.AQUA).component();
    }
    public static MutableComponent amount(long millibuckets) {
        return CreateLang.number(millibuckets).translate("generic.unit.millibuckets").style(ChatFormatting.GOLD).component();
    }
}
