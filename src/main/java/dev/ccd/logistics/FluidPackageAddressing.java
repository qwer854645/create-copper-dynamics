package dev.ccd.logistics;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.clipboard.ClipboardContent;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import com.simibubi.create.content.equipment.clipboard.ClipboardOverrides.ClipboardType;
import com.simibubi.create.content.logistics.box.PackageItem;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Inventory interaction using Create's clipboard address-book format and package address API. */
public final class FluidPackageAddressing {
    private FluidPackageAddressing() {}

    public static boolean interact(ItemStack parcel, ItemStack carried, Slot slot, ClickAction action, Player player) {
        if (!AllBlocks.CLIPBOARD.isIn(carried)) return false;
        // Consume the click even on protected slots so it cannot fall through to a swap.
        if (player.isSpectator() || !slot.allowModification(player)) return true;
        // Use the normal menu prediction path on both sides, including creative inventory clicks.
        ClipboardContent content = carried.getOrDefault(AllDataComponents.CLIPBOARD_CONTENT, ClipboardContent.EMPTY);
        if (action == ClickAction.SECONDARY) copy(parcel, carried, content, player);
        else paste(parcel, content, player);
        slot.setChanged();
        return true;
    }

    private static void copy(ItemStack parcel, ItemStack clipboard, ClipboardContent content, Player player) {
        if (content.readOnly()) {
            message(player, "read_only");
            return;
        }
        String address = PackageItem.getAddress(parcel);
        var pages = ClipboardEntry.readAll(content);
        if (pages.isEmpty()) pages.add(new ArrayList<>());
        int selected = Math.clamp(content.previouslyOpenedPage(), 0, pages.size() - 1);
        var entries = pages.get(selected);
        boolean found = false;
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            String text = entry.text.getString();
            if (!text.equals("#" + address) && !text.equals("# " + address)) continue;
            // ClipboardEntry is mutable: replace this entry without mutating the old component.
            if (entry.checked) entries.set(i, new ClipboardEntry(false, entry.text.copy())
                .displayItem(entry.icon.copy(), entry.itemAmount));
            found = true;
            break;
        }
        if (!found) {
            if (entries.size() >= 12) {
                entries = new ArrayList<>();
                pages.add(entries);
                selected = pages.size() - 1;
            }
            entries.add(new ClipboardEntry(false, Component.literal("#" + address)));
        }
        clipboard.set(AllDataComponents.CLIPBOARD_CONTENT,
            content.setPages(pages).setPreviouslyOpenedPage(selected).setType(ClipboardType.WRITTEN));
        message(player, address.isBlank() ? "copied_empty" : "copied", address);
    }

    private static void paste(ItemStack parcel, ClipboardContent content, Player player) {
        var pages = content.pages();
        var addresses = new LinkedHashSet<String>();
        if (!pages.isEmpty()) {
            int selected = Math.clamp(content.previouslyOpenedPage(), 0, pages.size() - 1);
            for (var entry : pages.get(selected)) {
                String text = entry.text.getString();
                if (!entry.checked && text.startsWith("#")) addresses.add(text.substring(1).trim());
            }
        }
        if (addresses.isEmpty()) {
            message(player, "missing");
            return;
        }
        if (addresses.size() != 1) {
            message(player, "ambiguous");
            return;
        }
        String address = addresses.iterator().next();
        // Touch only the native address component; cargo and order fragments stay intact.
        if (address.isEmpty()) PackageItem.clearAddress(parcel);
        else PackageItem.addAddress(parcel, address);
        message(player, address.isEmpty() ? "cleared" : "pasted", address);
    }

    private static void message(Player player, String action, Object... args) {
        if (player.level().isClientSide())
            player.displayClientMessage(Component.translatable("ccd.package.address." + action, args), true);
    }
}
