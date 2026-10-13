package stone.event;

import net.minecraft.network.Packet;

public abstract class PacketEvent extends Event {
    private final Packet<?> packet;

    protected PacketEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return packet;
    }
}
