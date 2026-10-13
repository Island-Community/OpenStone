package stone.module.shared;

import stone.module.Module;

public final class EverDisabledTracker {
    private final Module module;
    private boolean everDisabled;

    public EverDisabledTracker(Module var1) {
        this.module = var1;
        this.everDisabled = !var1.enabled();
    }

    public boolean hasEverBeenDisabled() {
        this.everDisabled = this.everDisabled | !this.module.enabled();
        return this.everDisabled;
    }
}
