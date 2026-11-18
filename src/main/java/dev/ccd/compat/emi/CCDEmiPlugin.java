package dev.ccd.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.ccd.compat.BrowserVisibility;

@EmiEntrypoint
public final class CCDEmiPlugin implements EmiPlugin {
    @Override public void register(EmiRegistry registry) {
        registry.removeEmiStacks(stack->BrowserVisibility.hidden(stack.getKey()));
    }
}
