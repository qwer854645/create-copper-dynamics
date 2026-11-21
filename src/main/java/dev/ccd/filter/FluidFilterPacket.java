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
            if(!dev.ccd.content.FluidFilterEditor.apply(data,menu.attributes,player.registryAccess(),packet.operation,packet.slot,packet.sample,packet.choice))return;
            menu.filter.set(CopperDynamics.FLUID_FILTER_DATA.get(), CustomData.of(data));
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        }));
    }
}
