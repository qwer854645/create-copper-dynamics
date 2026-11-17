package dev.ccd;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-side transport settings. Reopen the world after editing. */
public final class CCDConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue PUMP_STRESS_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue PUMP_PRESSURE_MULTIPLIER;
    public static final ModConfigSpec.IntValue PUMP_RANGE_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue ARM_ALLOW_DIRECT_CONTAINER_INTERACTION;
    public static final ModConfigSpec.IntValue SMART_TANK_MAX_FLUIDS;
    public static final ModConfigSpec.IntValue WATER_SUPPLY_RATE;

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
        builder.push("fluidArm");
        ARM_ALLOW_DIRECT_CONTAINER_INTERACTION = builder.comment(
            "Allow the fluid arm to interact directly with any sided fluid container.",
            "When false, inputs and outputs must be CCD fluid hatches. Hatch sided access and filtering still apply.")
            .worldRestart().define("allowDirectContainerInteraction", false);
        builder.pop();
        builder.push("smartTank");
        SMART_TANK_MAX_FLUIDS = builder.comment("Maximum distinct fluid/component combinations per formed smart-tank structure.",
            "Lowering this preserves existing contents; only new fluid kinds are rejected.")
            .worldRestart().defineInRange("maxFluidTypes", 8, 1, 64);
        builder.pop();
        builder.push("waterSupply");
        WATER_SUPPLY_RATE = builder.comment("Water per connected container per tick, delivered in batches every 5 ticks.",
            "-1 fills all available water capacity in one supply operation; 0 disables automatic supply.",
            "Idle/full targets are polled every 20 ticks. Manual water access is unaffected.")
            .worldRestart().defineInRange("rate", 1000, -1, Integer.MAX_VALUE);
        builder.pop();
        SPEC = builder.build();
    }

    public static int boosterRange(int nativeRange) {
        return (int) Math.min(Integer.MAX_VALUE, (long) nativeRange * PUMP_RANGE_MULTIPLIER.get());
    }

    private CCDConfig() {}
}
