package stone.util.packet;

import stone.event.DisconnectEvent;
import stone.event.EventBus;
import stone.event.LoadWorldEvent;
import stone.event.PacketReceiveEvent;
import stone.event.PacketSendEvent;
import stone.event.PostWalkingUpdateEvent;
import stone.event.Subscribe;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.S00PacketKeepAlive;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S19PacketEntityHeadLook;
import net.minecraft.network.play.server.S20PacketEntityProperties;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.network.play.server.S39PacketPlayerAbilities;

public final class BlinkComponent {
    private static final BlinkComponent INSTANCE = new BlinkComponent();
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static volatile boolean enabled;
    private static volatile long maxDelayMs;
    private static volatile long lastHoldTime;
    private static volatile boolean holdTransactions;
    private static volatile boolean holdVelocity;
    private static volatile boolean holdPosLook;
    private static volatile boolean holdEntities;

    private static final ConcurrentLinkedQueue<BlinkPacketEntry> queue = new ConcurrentLinkedQueue<>();
    private static volatile boolean dispatching;

    static {
        EventBus.register(INSTANCE);
    }

    private BlinkComponent() {}

    private record BlinkPacketEntry(Packet<?> packet, long timestamp, boolean outbound) {}

    public static void hold(int durationMs, boolean transactions, boolean velocity, boolean posLook, boolean entities) {
        maxDelayMs = durationMs;
        holdTransactions = transactions;
        holdVelocity = velocity;
        holdPosLook = posLook;
        holdEntities = entities;
        lastHoldTime = System.currentTimeMillis();
        enabled = true;
    }

    public static void a(int durationMs, boolean transactions, boolean velocity, boolean posLook, boolean entities) {
        hold(durationMs, transactions, velocity, posLook, entities);
    }

    public static void disable() {
        enabled = false;
    }

    public static void dispatch() {
        enabled = false;
        flushQueue();
    }

    public static void clear() {
        enabled = false;
        queue.clear();
    }

    public static boolean isBlinking() {
        return enabled && !queue.isEmpty();
    }

    public static int getQueuedCount() {
        return queue.size();
    }

    @Subscribe(priority = Subscribe.Priority.HIGH)
    public void onPacketSend(PacketSendEvent event) {
        if (!enabled || dispatching || event.cancelled()) {
            return;
        }
        Packet<?> packet = event.getPacket();
        if (shouldHoldOutbound(packet)) {
            event.setCancelled(true);
            queue.add(new BlinkPacketEntry(packet, System.currentTimeMillis(), true));
        }
    }

    @Subscribe(priority = Subscribe.Priority.HIGH)
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!enabled || dispatching || event.cancelled()) {
            return;
        }
        Packet<?> packet = event.getPacket();
        if (shouldHoldInbound(packet)) {
            event.setCancelled(true);
            queue.add(new BlinkPacketEntry(packet, System.currentTimeMillis(), false));
        }
    }

    @Subscribe
    public void onPostWalkingUpdate(PostWalkingUpdateEvent event) {
        long now = System.currentTimeMillis();
        if (enabled && now - lastHoldTime > 100L) {
            enabled = false;
        }
        drainExpired(now);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        clear();
    }

    @Subscribe
    public void onLoadWorld(LoadWorldEvent event) {
        clear();
    }

    private static boolean shouldHoldOutbound(Packet<?> packet) {
        if (holdTransactions) {
            if (packet instanceof C0FPacketConfirmTransaction || packet instanceof C00PacketKeepAlive) {
                return true;
            }
        }
        return false;
    }

    private static boolean shouldHoldInbound(Packet<?> packet) {
        if (holdTransactions) {
            if (packet instanceof S32PacketConfirmTransaction) {
                return true;
            }
        }
        if (holdVelocity) {
            if (packet instanceof S12PacketEntityVelocity || packet instanceof S27PacketExplosion) {
                return true;
            }
        }
        if (holdPosLook) {
            if (packet instanceof S08PacketPlayerPosLook || packet instanceof S39PacketPlayerAbilities || packet instanceof S00PacketKeepAlive) {
                return true;
            }
        }
        if (holdEntities) {
            if (packet instanceof S13PacketDestroyEntities
                    || packet instanceof S14PacketEntity
                    || packet instanceof S18PacketEntityTeleport
                    || packet instanceof S19PacketEntityHeadLook
                    || packet instanceof S20PacketEntityProperties) {
                return true;
            }
        }
        return false;
    }

    private static void drainExpired(long now) {
        if (queue.isEmpty()) {
            return;
        }
        List<BlinkPacketEntry> toRelease = new ArrayList<>();
        while (!queue.isEmpty()) {
            BlinkPacketEntry peek = queue.peek();
            if (peek != null && now - peek.timestamp >= maxDelayMs) {
                BlinkPacketEntry entry = queue.poll();
                if (entry != null) {
                    toRelease.add(entry);
                }
            } else {
                break;
            }
        }
        processEntries(toRelease);
    }

    private static void flushQueue() {
        if (queue.isEmpty()) {
            return;
        }
        List<BlinkPacketEntry> all = new ArrayList<>();
        BlinkPacketEntry entry;
        while ((entry = queue.poll()) != null) {
            all.add(entry);
        }
        processEntries(all);
    }

    private static void processEntries(List<BlinkPacketEntry> entries) {
        if (entries.isEmpty() || mc.thePlayer == null) {
            return;
        }
        dispatching = true;
        try {
            for (BlinkPacketEntry entry : entries) {
                if (entry.outbound) {
                    PacketUtils.sendSilently(entry.packet);
                } else {
                    PacketUtils.processInboundPacket(entry.packet);
                }
            }
        } finally {
            dispatching = false;
        }
    }
}
