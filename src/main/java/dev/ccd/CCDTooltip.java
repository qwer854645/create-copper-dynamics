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
        if (!tooltip.isEmpty()) tooltip.add(Component.empty());
        CreateLang.builder().add(Component.translatable(key)).forGoggles(tooltip);
    }
    public static void line(List<Component> tooltip, String key, Object... args) {
        line(tooltip, ChatFormatting.GRAY, key, args);
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
