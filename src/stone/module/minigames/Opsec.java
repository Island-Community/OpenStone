package stone.module.minigames;

import stone.event.Phase;
import stone.event.Subscribe;
import stone.event.TickEndEvent;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.util.ClientUtils;
import stone.util.packet.PacketDirection;
import stone.util.packet.PacketListenerRegistration;
import stone.util.packet.PacketReleaseGate;
import stone.util.packet.QueuedPacketDispatcher;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;

public final class Opsec extends Module {
    private final Setting pingBoost;
    private PacketListenerRegistration GNCBpp;
    private long zfC = -1L;
    private boolean inBedwarsLobby;

    public Opsec() {
        super("Opsec", "Adds ping in the Bed Wars lobby so it can't identify you", Category.MINIGAMES,
                Subcategory.ADVANCED);
        this.pingBoost = addNumber("Ping Boost", "", 100.0F, 50.0F, 1000.0F, 0);
    }

    @Override
    public void onEnable(Context context) {
        this.updateAntiPingGate();
    }

    @Override
    public void onDisable(Context context) {
        this.inBedwarsLobby = false;
        this.ojyUjf();
    }

    @Subscribe
    public void onTickEnd(TickEndEvent var1) {
        if (var1.phase == Phase.END) {
            this.updateAntiPingGate();
            if (this.GNCBpp != null) {
                QueuedPacketDispatcher.INSTANCE.Cvuz(PacketDirection.INBOUND, (long)this.pingBoost.number());
                QueuedPacketDispatcher.INSTANCE.Cvuz(PacketDirection.OUTBOUND, (long)this.pingBoost.number());
            }
        }
    }

    private void updateAntiPingGate() {
        if (this.enabled()) {
            if (this.GNCBpp != null && QueuedPacketDispatcher.INSTANCE != null && this.zfC != QueuedPacketDispatcher.INSTANCE.weqo1()) {
                this.ojyUjf();
            }

            this.inBedwarsLobby = this.detectGameState() == Opsec$2.BEDWARS_LOBBY;
            if (!this.inBedwarsLobby || mc.getNetHandler() == null || QueuedPacketDispatcher.INSTANCE == null) {
                this.ojyUjf();
            } else if (this.GNCBpp == null) {
                this.GNCBpp = new PacketListenerRegistration(PacketDirection.BIDIRECTIONAL, new PacketReleaseGate() {
                    @Override
                    protected boolean shouldRelease() {
                        return !Opsec.this.enabled() || !Opsec.this.inBedwarsLobby;
                    }
                });
                QueuedPacketDispatcher.INSTANCE.JUlwlNu(this.GNCBpp);
                this.zfC = QueuedPacketDispatcher.INSTANCE.weqo1();
            }
        } else {
            this.inBedwarsLobby = false;
            this.ojyUjf();
        }
    }

    private void ojyUjf() {
        if (this.GNCBpp != null) {
            this.GNCBpp.getPacketHandler().forceOpen();
            this.GNCBpp = null;
            this.zfC = -1L;
        }
    }

    private Opsec$2 detectGameState() {
        if (!ClientUtils.isInWorld()) {
            return Opsec$2.UNKNOWN;
        } else if (ClientUtils.isOnHypixel() && !ClientUtils.mdeQ()) {
            int var1 = ClientUtils.getBedWarsBoardType();
            if (var1 == 0) {
                return Opsec$2.BEDWARS_LOBBY;
            } else if (var1 != 1 && var1 != 2) {
                Scoreboard var2 = mc.theWorld.getScoreboard();
                if (var2 == null) {
                    return Opsec$2.UNKNOWN;
                } else {
                    ScoreObjective var3 = var2.getObjectiveInDisplaySlot(1);
                    if (var3 == null) {
                        return Opsec$2.UNKNOWN;
                    } else {
                        String var4 = ClientUtils.zaUnpz(var3.getDisplayName());
                        return var4.contains("BED WARS") ? Opsec$2.UNKNOWN : Opsec$2.NOT_BEDWARS_LOBBY;
                    }
                }
            } else {
                return Opsec$2.NOT_BEDWARS_LOBBY;
            }
        } else {
            return Opsec$2.NOT_BEDWARS_LOBBY;
        }
    }
}
