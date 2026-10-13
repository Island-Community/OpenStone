package stone.event;

import net.minecraft.network.Packet;

public final class PacketSendEvent extends PacketEvent {
    public PacketSendEvent(Packet<?> packet) {
        super(packet);
    }
}
