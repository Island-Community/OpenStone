package stone.util.packet;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.network.Packet;

public final class PacketExclusions {
    private final Set<Packet<?>> outboundPackets = newPacketIdentitySet();

    public void excludeOutboundPacket(Packet<?> var1) {
        this.outboundPackets.add(var1);
    }

    public boolean consumeOutboundPacket(Packet<?> var1) {
        return this.outboundPackets.remove(var1);
    }

    private static Set<Packet<?>> newPacketIdentitySet() {
        IdentityHashMap var0 = new IdentityHashMap();
        return Collections.newSetFromMap(Collections.synchronizedMap(var0));
    }
}
