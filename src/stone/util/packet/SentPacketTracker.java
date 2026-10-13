package stone.util.packet;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.network.Packet;

public final class SentPacketTracker {
    private final Set<Packet<?>> trackedPackets = Collections.newSetFromMap(Collections.synchronizedMap(new IdentityHashMap<>()));

    public void track(Packet<?> var1) {
        this.trackedPackets.add(var1);
    }

    public boolean untrack(Packet<?> var1) {
        return this.trackedPackets.remove(var1);
    }

    public void PWbx34() {
        this.trackedPackets.clear();
    }
}
