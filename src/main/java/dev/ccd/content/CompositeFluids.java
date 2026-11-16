package dev.ccd.content;

import java.util.ArrayList;
import java.util.List;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Immutable list of directly carried fluids; item contents stay in Create's component. */
public record CompositeFluids(int version, List<FluidCargo> entries) {
    public static final int VERSION = 1;
    public static final Codec<CompositeFluids> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.intRange(1, VERSION).fieldOf("version").forGetter(CompositeFluids::version),
        FluidCargo.CODEC.listOf(0, CompositePackageItem.MAX_FLUID_TYPES).fieldOf("fluids").forGetter(CompositeFluids::entries)
    ).apply(instance, CompositeFluids::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CompositeFluids> STREAM_CODEC = StreamCodec.of(
        (buffer, data) -> {
            buffer.writeVarInt(data.version()); buffer.writeVarInt(data.entries().size());
            for (FluidCargo fluid : data.entries()) FluidCargo.STREAM_CODEC.encode(buffer, fluid);
        }, buffer -> {
            int version = buffer.readVarInt();
            int count = buffer.readVarInt();
            if (version != VERSION || count < 0 || count > CompositePackageItem.MAX_FLUID_TYPES)
                throw new DecoderException("Unsupported composite fluid data");
            List<FluidCargo> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) entries.add(FluidCargo.STREAM_CODEC.decode(buffer));
            return new CompositeFluids(version, entries);
        });
    public CompositeFluids {
        if (version != VERSION || entries.size() > CompositePackageItem.MAX_FLUID_TYPES)
            throw new IllegalArgumentException("Invalid composite fluid data");
        entries = List.copyOf(entries);
    }
    public CompositeFluids(List<FluidCargo> entries) { this(VERSION, entries); }
}
