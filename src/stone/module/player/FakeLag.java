package stone.module.player;

import stone.event.Phase;
import stone.event.PlayerAttackEvent;
import stone.event.RenderWorldLastEvent;
import stone.event.Subscribe;
import stone.event.TickEndEvent;
import stone.event.TickStartEvent;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.ModuleManager;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.shared.DisabledOrNestedCondition;
import stone.module.shared.ProgressBarSource;
import stone.ui.Client;
import stone.util.ClientUtils;
import stone.util.packet.PacketDirection;
import stone.util.packet.PacketListenerRegistration;
import stone.util.packet.QueuedPacketDispatcher;
import stone.util.render.FakePlayerRenderer;
import stone.util.render.FakePlayerRenderer$1;
import stone.util.render.FakePlayerRenderer$2;
import java.util.List;
import java.util.Set;

public class FakeLag extends Module implements ProgressBarSource {
    private static final String[] wagB = new String[]{"Inbound", "Outbound", "Both"};
    private final Setting mode;
    private final Setting packetDelay;
    private final Setting disableLagrange;
    private final Setting attack;
    private final Setting showServerPosition;
    private int appliedMode = -1;
    private long appliedPacketDelay = -1L;
    private PacketListenerRegistration XBz;
    private long lagStartTime;
    private boolean UuG;
    private final FakePlayerRenderer$1 serverPositionTracker = new FakePlayerRenderer$1();

    public FakeLag() {
        super("FakeLag", "Disable on", Category.PLAYER, Subcategory.EXTREME);
        this.mode = addChoice("Mode", "", List.of(wagB), 1);
        this.packetDelay = addNumber("Packet delay", "", 0.0F, 0.0F, 1500.0F, 0);
        this.disableLagrange = addBoolean("Disable Lagrange", "", false);
        this.attack = addBoolean("Attack", "", false);
        this.showServerPosition = addBoolean("Show server position", "", false);
    }

    @Override
    public String suffix() {
        return (int)this.packetDelay.number() + "ms";
    }

    @Override
    public boolean isProgressActive() {
        return this.enabled() && this.packetDelay.number() > 0.0;
    }

    @Override
    public float getProgressFraction() {
        if (this.UuG) {
            return 1.0F;
        } else {
            long var1 = Math.max(1L, (long)this.packetDelay.number());
            if (this.lagStartTime <= 0L) {
                return 0.0F;
            } else {
                long var3 = System.currentTimeMillis() - this.lagStartTime;
                return Math.max(0.0F, Math.min(1.0F, (float)var3 / (float)var1));
            }
        }
    }

    @Override
    public String getProgressLabel() {
        return "Fake Lag";
    }

    @Override
    public void guiUpdate() {
        this.disableLagrange.setVisible(this.isOutboundMode());
        if (this.enabled()) {
            if (this.packetDelay.number() <= 0.0) {
                setEnabled(false, Client.context());
            } else {
                int var1 = (int)this.mode.choice();
                long var2 = (long)this.packetDelay.number();
                if (var1 != this.appliedMode || var2 != this.appliedPacketDelay) {
                    this.appliedMode = var1;
                    this.appliedPacketDelay = var2;
                    this.rebuildPacketQueue();
                }
            }
        }
    }

    private void rebuildPacketQueue() {
        if (this.XBz != null) {
            this.XBz.getPacketHandler().forceOpen();
        }

        this.XBz = new PacketListenerRegistration(this.getSelectedDirections(), new DisabledOrNestedCondition(this));
        QueuedPacketDispatcher.INSTANCE.JUlwlNu(this.XBz);
    }

    private Set<PacketDirection> getSelectedDirections() {
        switch ((int)this.mode.choice()) {
            case 0:
                return PacketDirection.ONLY_INBOUND;
            case 1:
            default:
                return PacketDirection.ONLY_OUTBOUND;
            case 2:
                return PacketDirection.BIDIRECTIONAL;
        }
    }

    public boolean isOutboundMode() {
        int var1 = (int)this.mode.choice();
        return var1 == 1 || var1 == 2;
    }

    public boolean isLagrangeDisableActive() {
        return this.enabled() && this.disableLagrange.bool() && this.XBz != null && this.packetDelay.number() > 0.0 && this.isOutboundMode();
    }

    @Override
    public void onEnable(Context context) {
        if (mc.isSingleplayer()) {
            ClientUtils.sendTitledMessage("Stone", "&fFake Lag &7cannot be enabled in singleplayer.");
            setEnabled(false, Client.context());
        } else if (ModuleManager.get(Blink.class).enabled()) {
            ClientUtils.sendColoredMessage("&cCannot use Fake Lag with Blink!");
            setEnabled(false, Client.context());
        } else {
            this.appliedMode = (int)this.mode.choice();
            this.appliedPacketDelay = (long)this.packetDelay.number();
            this.lagStartTime = System.currentTimeMillis();
            this.UuG = false;
            this.resetServerPositionTracker();
            this.rebuildPacketQueue();
        }
    }

    @Override
    public void onDisable(Context context) {
        if (this.XBz != null) {
            this.XBz.getPacketHandler().forceOpen();
            this.XBz = null;
        }

        this.appliedMode = -1;
        this.appliedPacketDelay = -1L;
        this.lagStartTime = 0L;
        this.UuG = false;
        this.resetServerPositionTracker();
    }

    @Subscribe
    public void onTickStart(TickStartEvent var1) {
        if (this.enabled()) {
            if (ClientUtils.isInWorld() && mc.theWorld != null) {
                long var2 = (long)this.packetDelay.number();
                if (var2 > 0L) {
                    Set var4 = this.getSelectedDirections();
                    if (var4.contains(PacketDirection.INBOUND)) {
                        QueuedPacketDispatcher.INSTANCE.Cvuz(PacketDirection.INBOUND, var2);
                    }

                    if (var4.contains(PacketDirection.OUTBOUND)) {
                        QueuedPacketDispatcher.INSTANCE.Cvuz(PacketDirection.OUTBOUND, var2);
                    }

                    long var5 = System.currentTimeMillis();
                    if (this.lagStartTime <= 0L) {
                        this.lagStartTime = var5;
                    } else if (var5 - this.lagStartTime >= var2) {
                        this.UuG = true;
                    }
                }
            } else {
                setEnabled(false, Client.context());
            }
        }
    }

    @Subscribe
    public void onPlayerAttack(PlayerAttackEvent var1) {
        if (this.enabled() && this.attack.bool() && ClientUtils.isInWorld()) {
            if (var1.entityPlayer == mc.thePlayer) {
                setEnabled(false, Client.context());
            }
        }
    }

    @Subscribe
    public void onRenderWorldLast(RenderWorldLastEvent var1) {
        if (!this.enabled() || !this.showServerPosition.bool() || !ClientUtils.isInWorld()) {
            this.resetServerPositionTracker();
        } else if (!this.isOutboundMode()) {
            this.resetServerPositionTracker();
        } else {
            long var2 = (long)this.packetDelay.number();
            this.serverPositionTracker.recordSnapshot(mc.thePlayer, var1.partialTicks);
            FakePlayerRenderer$2 var4 = this.serverPositionTracker.YJfvaQ(var2);
            FakePlayerRenderer.renderFakePlayerWithDefaultAlpha(mc.thePlayer, var4, var1.partialTicks);
        }
    }

    @Subscribe
    public void onTickEnd(TickEndEvent var1) {
        if (var1.phase == Phase.END) {
            if (mc.theWorld == null && this.enabled()) {
                setEnabled(false, Client.context());
            }
        }
    }

    private void resetServerPositionTracker() {
        this.serverPositionTracker.clearHistory();
    }

}
