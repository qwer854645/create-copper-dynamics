package dev.ccd.compat.kubejs;

import dev.ccd.api.AtomizerScripts;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.ScriptManager;
import dev.latvian.mods.kubejs.script.ScriptType;

/** Loaded by KubeJS plugin discovery only; the mod's common code never references KubeJS classes. */
public final class CCDKubeJSPlugin implements KubeJSPlugin {
    @Override public void registerBindings(BindingRegistry bindings) {
        if (bindings.type() == ScriptType.SERVER) {
            bindings.add("CCDAtomizer", AtomizerScripts.class);
            bindings.add("CCDFluids",dev.ccd.api.FluidWorldScripts.class);
        }
    }
    @Override public void beforeScriptsLoaded(ScriptManager manager) {
        if (manager.scriptType == ScriptType.SERVER) { AtomizerScripts.clear(); dev.ccd.api.FluidWorldScripts.clear(); }
    }
}
