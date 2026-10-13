package stone.module.player.blink;

import stone.util.packet.PacketDirection;
import java.util.Set;

public final class BlinkMode {

    private BlinkMode() {
    }

    public static Set<PacketDirection> getPacketDirections(int var0) {
        if (var0 == 0) {
            return PacketDirection.ONLY_INBOUND;
        } else {
            return var0 == 2 ? PacketDirection.BIDIRECTIONAL : PacketDirection.ONLY_OUTBOUND;
        }
    }

    public static boolean vTkoy(int var0) {
        return var0 == 0 || var0 == 2;
    }

    public static boolean isOutbound(int var0) {
        return var0 == 1 || var0 == 2;
    }
}
