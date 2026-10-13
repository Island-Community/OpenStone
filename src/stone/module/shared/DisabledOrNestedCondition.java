package stone.module.shared;

import stone.module.Module;
import stone.util.packet.PacketReleaseGate;

public final class DisabledOrNestedCondition extends PacketReleaseGate {
    private final EverDisabledTracker everDisabledTracker;
    private final PacketReleaseGate j53;

    public DisabledOrNestedCondition(Module var1, PacketReleaseGate var2) {
        this.everDisabledTracker = new EverDisabledTracker(var1);
        this.j53 = var2;
    }

    public DisabledOrNestedCondition(Module var1) {
        this(var1, null);
    }

    @Override
    protected boolean shouldRelease() {
        return this.everDisabledTracker.hasEverBeenDisabled() || this.j53 != null && this.j53.isOpen();
    }
}
