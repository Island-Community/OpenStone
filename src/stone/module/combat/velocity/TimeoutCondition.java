package stone.module.combat.velocity;

import stone.util.packet.PacketReleaseGate;

public final class TimeoutCondition extends PacketReleaseGate {
    private final long JDl;

    public TimeoutCondition(long var1) {
        this.JDl = System.currentTimeMillis() + Math.max(1L, var1);
    }

    @Override
    protected boolean shouldRelease() {
        return System.currentTimeMillis() >= this.JDl;
    }
}
