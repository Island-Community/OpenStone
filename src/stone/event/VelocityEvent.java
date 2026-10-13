package stone.event;

import net.minecraft.network.play.server.S12PacketEntityVelocity;

public final class VelocityEvent extends Event {
    public final S12PacketEntityVelocity packet;

    public VelocityEvent(S12PacketEntityVelocity packet) {
        this.packet = packet;
    }
}
