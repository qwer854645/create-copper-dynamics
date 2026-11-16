package dev.ccd.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;

/** Immutable component: callers never receive the mutable FluidStack stored inside. */
public record FluidCargo(int version, FluidStack fluid) {
    public static final int VERSION = 1;
    public static final Codec<FluidCargo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.intRange(1, VERSION).fieldOf("version").forGetter(FluidCargo::version),
        FluidStack.CODEC.fieldOf("fluid").forGetter(FluidCargo::fluid)
    ).apply(instance, FluidCargo::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidCargo> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, FluidCargo::version,
        FluidStack.STREAM_CODEC, FluidCargo::fluid, FluidCargo::new);

    public FluidCargo {
        if (version != VERSION || fluid.isEmpty()) throw new IllegalArgumentException("Invalid fluid cargo");
        fluid = fluid.copy();
    }

    public FluidCargo(FluidStack fluid) { this(VERSION, fluid); }
    @Override public FluidStack fluid() { return fluid.copy(); }
    @Override public boolean equals(Object other) {
        return other instanceof FluidCargo cargo && version == cargo.version && FluidStack.matches(fluid, cargo.fluid);
    }
    @Override public int hashCode() {
        return 31 * (31 * version + FluidStack.hashFluidAndComponents(fluid)) + fluid.getAmount();
    }
}
