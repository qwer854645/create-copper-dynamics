package dev.ccd;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Common config must load before recipes; runtime visibility follows the server on login. */
@EventBusSubscriber(modid=CopperDynamics.ID)
public record CCDFeatureSync(Map<String,Boolean> settings) implements CustomPacketPayload {
    public CCDFeatureSync { settings=Map.copyOf(settings); }
    public static final Type<CCDFeatureSync> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("ccd","content_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf,CCDFeatureSync> CODEC=StreamCodec.of((buffer,payload)->{
        buffer.writeVarInt(payload.settings.size());
        payload.settings.forEach((key,value)->{buffer.writeUtf(key,80);buffer.writeBoolean(value);});
    },buffer->{
        int count=buffer.readVarInt();
        if(count<0||count>128)throw new io.netty.handler.codec.DecoderException("Invalid CCD content settings");
        var values=new LinkedHashMap<String,Boolean>();
        for(int i=0;i<count;i++)values.put(buffer.readUtf(80),buffer.readBoolean());
        return new CCDFeatureSync(values);
    });
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent event){
        event.registrar("1").playToClient(TYPE,CODEC,(packet,context)->context.enqueueWork(()->CCDFeatures.remote(packet.settings)));
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player){
            var values=new LinkedHashMap<String,Boolean>();
            CCDFeatures.CONTENT.forEach((key,value)->values.put(key,value.get()));
            CCDFeatures.COMPAT_FLUID_MODES.forEach((key,value)->values.put(key,CCDFeatures.enabled(key)));
            PacketDistributor.sendToPlayer(player,new CCDFeatureSync(values));
        }
    }
}
