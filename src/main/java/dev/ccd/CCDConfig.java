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
    public static final ModConfigSpec.IntValue ATOMIZER_NORMAL_COST, ATOMIZER_POTION_COST;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.translation("ccd.configuration.boosterPump").push("boosterPump");
        PUMP_STRESS_MULTIPLIER = builder.comment("Stress cost as a multiple of a Mechanical Pump at the same speed.")
            .translation("ccd.configuration.boosterPump.stressMultiplier")
            .worldRestart().defineInRange("stressMultiplier", 1.5, 1.0, 16.0);
        PUMP_PRESSURE_MULTIPLIER = builder.comment("Pumping pressure as a multiple of a Mechanical Pump at the same speed. Higher values increase flow rate.")
            .translation("ccd.configuration.boosterPump.pressureMultiplier")
            .worldRestart().defineInRange("pressureMultiplier", 2.0, 1.0, 16.0);
        PUMP_RANGE_MULTIPLIER = builder.comment("Pumping distance on either side as a multiple of the Mechanical Pump's configured range.")
            .translation("ccd.configuration.boosterPump.rangeMultiplier")
            .worldRestart().defineInRange("rangeMultiplier", 2, 1, 8);
        builder.pop();
        builder.translation("ccd.configuration.fluidArm").push("fluidArm");
        ARM_ALLOW_DIRECT_CONTAINER_INTERACTION = builder.comment(
            "Allow fluid arms to use fluid containers directly instead of requiring Fluid Hatches.",
            "Container faces and fluid filters still restrict transfer.")
            .translation("ccd.configuration.fluidArm.allowDirectContainerInteraction")
            .worldRestart().define("allowDirectContainerInteraction", false);
        builder.pop();
        builder.translation("ccd.configuration.smartTank").push("smartTank");
        SMART_TANK_MAX_FLUIDS = builder.comment("Maximum fluid types in one connected Smart Fluid Tank. Different fluid properties count separately.",
            "Lowering this limit keeps existing contents, but prevents additional types from being inserted.")
            .translation("ccd.configuration.smartTank.maxFluidTypes")
            .worldRestart().defineInRange("maxFluidTypes", 8, 1, 64);
        builder.pop();
        builder.translation("ccd.configuration.waterSupply").push("waterSupply");
        WATER_SUPPLY_RATE = builder.comment("[in mB/t per adjacent container] Automatic water supply rate.",
            "-1 fills the available capacity in one operation. 0 disables automatic supply.",
            "Manual filling remains available. Water is supplied periodically rather than every tick.")
            .translation("ccd.configuration.waterSupply.rate")
            .worldRestart().defineInRange("rate", 1000, -1, Integer.MAX_VALUE);
        builder.pop();
        builder.translation("ccd.configuration.atomizer").push("atomizer");
        ATOMIZER_NORMAL_COST = builder.comment("[in mB per second] Fluid consumed when atomizing a fan-processing medium. Custom recipes may override this.")
            .translation("ccd.configuration.atomizer.normalConsumption")
            .worldRestart().defineInRange("normalConsumption", 10, 1, 4000);
        ATOMIZER_POTION_COST = builder.comment("[in mB per second] Potion consumed while spraying potion effects. Custom recipes may override this.")
            .translation("ccd.configuration.atomizer.potionConsumption")
            .worldRestart().defineInRange("potionConsumption", 25, 1, 4000);
        builder.pop();
        SPEC = builder.build();
    }

    public static int boosterRange(int nativeRange) {
        return (int) Math.min(Integer.MAX_VALUE, (long) nativeRange * PUMP_RANGE_MULTIPLIER.get());
    }

    private CCDConfig() {}
}
