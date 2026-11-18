package dev.ccd.client;

import com.simibubi.create.content.fluids.tank.FluidTankCTBehaviour;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

/** Native tank connectivity and connected textures, with CCD's brass outer and inner materials. */
public final class SmartFluidTankModels {
    private static final FluidTankCTBehaviour BEHAVIOUR = new FluidTankCTBehaviour(
        shift("fluid_tank"), shift("fluid_tank_top"), shift("fluid_tank_inner"));

    private static CTSpriteShiftEntry shift(String name) {
        var base = ResourceLocation.fromNamespaceAndPath("ccd", "block/smart_fluid_tank/" + name);
        return CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, base, base.withSuffix("_connected"));
    }

    public static void registerModels() {} // Register sprite shifts before atlas loading.
    public static BakedModel wrap(BakedModel original) { return new CTModel(original, BEHAVIOUR); }
}
