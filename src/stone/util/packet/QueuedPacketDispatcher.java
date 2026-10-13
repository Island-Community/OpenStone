package stone.util.packet;

import stone.event.Event;
import stone.event.EventBus;
import stone.event.PacketReceiveEvent;
import stone.event.PacketSendEvent;
import stone.event.Subscribe;
import stone.event.TickStartEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C01PacketChatMessage;

public final class QueuedPacketDispatcher extends HandledPacketTracker {
    public static final QueuedPacketDispatcher INSTANCE = new QueuedPacketDispatcher();

    private QueuedPacketDispatcher() {
        EventBus.register(this);
    }

    private final PacketQueueManager JQwj = new PacketQueueManager(this);
    private final SentPacketTracker sentPacketTracker = new SentPacketTracker();
    private volatile long generation;

    public void JUlwlNu(PacketListenerRegistration var1) {
        this.JQwj.enqueueEntry(var1);
    }

    public void Cvuz(PacketDirection var1, long var2) {
        this.JQwj.gfXr(var1, var2);
    }

    public long weqo1() {
        return this.generation;
    }

    public synchronized void FfRco() {
        this.JQwj.clearQueues();
        this.sentPacketTracker.PWbx34();
        this.generation++;
    }

    private boolean canDispatchPackets() {
        if (Minecraft.getMinecraft().getNetHandler() != null) {
            return true;
        } else {
            this.FfRco();
            return false;
        }
    }

    @Subscribe(priority = Subscribe.Priority.LOWEST)
    public void onPacketSend(PacketSendEvent var1) {
        if (this.canDispatchPackets()) {
            this.dispatchPacket(var1.getPacket(), var1, PacketDirection.OUTBOUND);
        }
    }

    @Subscribe
    public void onPacketReceive(PacketReceiveEvent var1) {
        if (this.canDispatchPackets()) {
            this.dispatchPacket(var1.getPacket(), var1, PacketDirection.INBOUND);
        }
    }

    @Subscribe
    public void onTickStart(TickStartEvent var1) {
        if (this.canDispatchPackets()) {
            this.JQwj.submitPacket(null, null);
        }
    }

    @Override
    public void markHandled(Packet<?> var1) {
        this.sentPacketTracker.track(var1);
    }

    private void dispatchPacket(Packet<?> var1, Event var2, PacketDirection var3) {
        boolean var4 = var3 == PacketDirection.OUTBOUND;
        EnumPacketDirection var5 = var4 ? EnumPacketDirection.SERVERBOUND : EnumPacketDirection.CLIENTBOUND;
        if (EnumConnectionState.PLAY.getPacketId(var5, var1) != null) {
            boolean var6 = this.sentPacketTracker.untrack(var1);
            if (!var2.cancelled() && !var6 && (!var4 || !(var1 instanceof C01PacketChatMessage)) && this.JQwj.submitPacket(var1, var3)) {
                var2.setCancelled(true);
            }
        }
    }
}
