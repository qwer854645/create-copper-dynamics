package dev.ccd;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;

public record FeatureEnabledCondition(String feature) implements ICondition {
    public static final MapCodec<FeatureEnabledCondition> CODEC = Codec.STRING.fieldOf("feature").xmap(FeatureEnabledCondition::new, FeatureEnabledCondition::feature);
    @Override public boolean test(IContext context) { return CCDFeatures.enabled(feature); }
    @Override public MapCodec<? extends ICondition> codec() { return CODEC; }
}
