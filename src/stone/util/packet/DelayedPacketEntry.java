package stone.util.packet;

import net.minecraft.network.Packet;

public final class DelayedPacketEntry extends PacketQueueEntry {
    private final Packet<?> packet;
    private final PacketDirection packetDirection;
    private final long UpNmu;

    public DelayedPacketEntry(Packet<?> var1, PacketDirection var2) {
        this.packet = var1;
        this.packetDirection = var2;
        this.UpNmu = System.currentTimeMillis();
    }

    @Override
    public boolean isExpired(long var1) {
        return this.UpNmu <= var1;
    }

    @Override
    public PacketListenerRegistration nok9(PacketListenerRegistration var1, HandledPacketTracker var2) {
        this.GuVw(var2);
        return var1;
    }

    public void GuVw(HandledPacketTracker var1) {
        if (PacketDirection.OUTBOUND == this.packetDirection) {
            var1.markHandled(this.packet);
        }

        this.packetDirection.passThroughChannel(this.packet);
    }
}
