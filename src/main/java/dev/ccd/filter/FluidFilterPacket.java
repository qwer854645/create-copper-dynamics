package dev.ccd.filter;

import dev.ccd.CopperDynamics;
import dev.ccd.content.FluidFilterItem;
import dev.ccd.content.FluidFilterMenu;
import dev.ccd.content.FluidFilterRules;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Only edits the filter locked by the player's currently open menu. */
public record FluidFilterPacket(int menu, int operation, int slot, FluidStack sample, int choice) implements CustomPacketPayload {
    public static final Type<FluidFilterPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("ccd", "fluid_filter"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidFilterPacket> CODEC = new StreamCodec<>() {
        @Override public FluidFilterPacket decode(RegistryFriendlyByteBuf b) {
            return new FluidFilterPacket(b.readVarInt(), b.readVarInt(), b.readVarInt(), FluidStack.OPTIONAL_STREAM_CODEC.decode(b), b.readVarInt());
        }
        @Override public void encode(RegistryFriendlyByteBuf b, FluidFilterPacket p) {
            b.writeVarInt(p.menu); b.writeVarInt(p.operation); b.writeVarInt(p.slot);
            FluidStack.OPTIONAL_STREAM_CODEC.encode(b, p.sample); b.writeVarInt(p.choice);
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, CODEC, (packet, context) -> context.enqueueWork(() -> {
            var player = context.player();
            if (!(player.containerMenu instanceof FluidFilterMenu menu) || menu.containerId != packet.menu || !menu.stillValid(player)) return;
            var data = FluidFilterItem.data(menu.filter);
            var entries = data.getList("entries", net.minecraft.nbt.Tag.TAG_COMPOUND).copy();
            if (packet.operation == 0 && packet.slot >= 0 && packet.slot < FluidFilterRules.SLOTS && !packet.sample.isEmpty()) {
                var entry = new net.minecraft.nbt.CompoundTag();
                if (menu.attributes) {
                    var options = FluidFilterRules.choices(packet.sample);
                    if (packet.choice < 0 || packet.choice >= options.size()) return;
                    entry = options.get(packet.choice).copy();
                }
                entry.put("fluid", packet.sample.copyWithAmount(1).saveOptional(player.registryAccess()));
                if (packet.slot < entries.size()) entries.set(packet.slot, entry);
                else if (entries.size() < FluidFilterRules.SLOTS) entries.add(entry);
            } else if (packet.operation == 1 && packet.slot >= 0 && packet.slot < entries.size()) entries.remove(packet.slot);
            else if (packet.operation == 2) data.putBoolean("blacklist", !data.getBoolean("blacklist"));
            else if (packet.operation == 3) data.putBoolean(menu.attributes ? "all" : "components", !data.getBoolean(menu.attributes ? "all" : "components"));
            else if (packet.operation == 4 && menu.attributes && packet.slot >= 0 && packet.slot < entries.size()) {
                var entry = entries.getCompound(packet.slot); entry.putBoolean("invert", !entry.getBoolean("invert"));
            } else if(packet.operation==5)entries.clear();
            else return;
            data.put("entries", entries);
            menu.filter.set(CopperDynamics.FLUID_FILTER_DATA.get(), CustomData.of(data));
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        }));
    }
}
