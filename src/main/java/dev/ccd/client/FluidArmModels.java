package dev.ccd.client;

import com.simibubi.create.AllPartialModels;
import dev.ccd.CopperDynamics;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** Scoped arm materials and syringe head, retaining Create's native joint animation. */
public final class FluidArmModels {
    private static final Map<PartialModel, PartialModel> MODELS = new IdentityHashMap<>();
    static {
        add(AllPartialModels.ARM_LOWER_BODY, "lower_body");
        add(AllPartialModels.ARM_UPPER_BODY, "upper_body");
        add(AllPartialModels.ARM_CLAW_BASE, "claw_base");
        add(AllPartialModels.ARM_CLAW_BASE_GOGGLES, "claw_base_goggles");
        add(AllPartialModels.ARM_CLAW_GRIP_UPPER, "upper_claw_grip");
        add(AllPartialModels.ARM_CLAW_GRIP_LOWER, "lower_claw_grip");
    }
    private static void add(PartialModel source, String name) {
        MODELS.put(source, PartialModel.of(ResourceLocation.fromNamespaceAndPath("ccd", "block/fluid_arm/" + name)));
    }
    public static void registerModels() {} // Hold all partials strongly before baking.
    public static PartialModel select(PartialModel original, BlockState state) {
        return state.is(CopperDynamics.FLUID_ARM.get()) ? MODELS.getOrDefault(original, original) : original;
    }
}
