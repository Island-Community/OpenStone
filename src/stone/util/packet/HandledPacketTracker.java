package stone.util.packet;

import net.minecraft.network.Packet;

public abstract class HandledPacketTracker {
    public abstract void markHandled(Packet<?> var1);
}
