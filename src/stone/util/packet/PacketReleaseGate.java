package stone.util.packet;

import stone.util.OneWayFlag;

public abstract class PacketReleaseGate {
    private final OneWayFlag dGx = new OneWayFlag();

    protected abstract boolean shouldRelease();

    public final boolean isOpen() {
        return this.dGx.isSet() ? true : this.shouldRelease();
    }

    public final void forceOpen() {
        this.dGx.DzqT();
    }
}
