package dev.ccd;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Multipliers follow the server's native pump settings. Reopen the world after editing. */
public final class CCDConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue PUMP_STRESS_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue PUMP_PRESSURE_MULTIPLIER;
    public static final ModConfigSpec.IntValue PUMP_RANGE_MULTIPLIER;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("boosterPump");
        PUMP_STRESS_MULTIPLIER = builder.comment("Stress impact relative to Create's mechanical pump.")
            .worldRestart().defineInRange("stressMultiplier", 1.5, 1.0, 16.0);
        PUMP_PRESSURE_MULTIPLIER = builder.comment("Pressure multiplier at the same actual RPM; also increases native flow rate.")
            .worldRestart().defineInRange("pressureMultiplier", 2.0, 1.0, 16.0);
        PUMP_RANGE_MULTIPLIER = builder.comment("Range on each fluid side, relative to Create's configured mechanical pump range.")
            .worldRestart().defineInRange("rangeMultiplier", 2, 1, 8);
        builder.pop();
        SPEC = builder.build();
    }

    public static int boosterRange(int nativeRange) {
        return (int) Math.min(Integer.MAX_VALUE, (long) nativeRange * PUMP_RANGE_MULTIPLIER.get());
    }

    private CCDConfig() {}
}
