package stone.module.player;

import stone.event.PlayerAttackEvent;
import stone.event.PreUpdateEvent;
import stone.event.RenderWorldLastEvent;
import stone.event.Subscribe;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.player.blink.BlinkMode;
import stone.module.player.blink.ElapsedTimer;
import stone.module.shared.DisabledOrNestedCondition;
import stone.module.shared.ProgressBarSource;
import stone.ui.Client;
import stone.util.ClientUtils;
import stone.util.packet.PacketListenerRegistration;
import stone.util.packet.QueuedPacketDispatcher;
import stone.util.render.FakePlayerRenderer;
import java.util.List;
import net.minecraft.util.Vec3;

public class Blink extends Module implements ProgressBarSource {
    private static final String[] MODE_NAMES = new String[]{"Inbound", "Outbound", "Both"};
    private Setting mode;
    private Setting disableAfter;
    private Setting maxDuration;
    private Setting disableLagrange;
    private Setting attack;
    private Setting showInitialPosition;
    private Vec3 vec3;
    private int activeTicks;
    private final ElapsedTimer WPA = new ElapsedTimer();

    public Blink() {
        super("Blink", "Disable on", Category.PLAYER, Subcategory.EXTREME);
        this.mode = addChoice("Mode", "", List.of(MODE_NAMES), 1);
        this.disableLagrange = addBoolean("Disable Lagrange", "", false);
        this.maxDuration = addBoolean("Max duration", "", false);
        this.disableAfter = addNumber("Disable after", "", 500.0F, 50.0F, 20000.0F, 0);
        this.attack = addBoolean("Attack", "", false);
        this.showInitialPosition = addBoolean("Show initial position", "", true);
    }

    @Override
    public void guiUpdate() {
        this.disableLagrange.setVisible(this.isOutboundMode());
        this.disableAfter.setVisible(this.maxDuration.bool());
    }

    @Override
    public void onEnable(Context context) {
        this.vec3 = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        this.activeTicks = 0;
        this.WPA.startAt(System.currentTimeMillis());
        QueuedPacketDispatcher.INSTANCE.JUlwlNu(new PacketListenerRegistration(BlinkMode.getPacketDirections(this.getModeIndex()), new DisabledOrNestedCondition(this)));
    }

    public boolean isInboundMode() {
        return BlinkMode.vTkoy(this.getModeIndex());
    }

    public boolean isOutboundMode() {
        return BlinkMode.isOutbound(this.getModeIndex());
    }

    public boolean shouldDisableLagrange() {
        return this.enabled() && this.disableLagrange.bool() && this.isOutboundMode();
    }

    @Override
    public String suffix() {
        return String.valueOf(this.activeTicks);
    }

    @Override
    public boolean isProgressActive() {
        return this.enabled() && this.maxDuration.bool();
    }

    @Override
    public float getProgressFraction() {
        return !this.maxDuration.bool() ? 0.0F : this.WPA.getProgressFraction(System.currentTimeMillis(), (long)this.disableAfter.number());
    }

    @Override
    public String getProgressLabel() {
        return "Blink";
    }

    @Subscribe
    public void onPreUpdate(PreUpdateEvent var1) {
        this.activeTicks++;
        if (this.maxDuration.bool()) {
            if (this.WPA.hasElapsed(System.currentTimeMillis(), (int)this.disableAfter.number())) {
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
        if (ClientUtils.isInWorld() && this.vec3 != null && this.showInitialPosition.bool() && this.isOutboundMode()) {
            FakePlayerRenderer.renderAtPosition(mc.thePlayer, this.vec3, var1.partialTicks);
        }
    }

    private int getModeIndex() {
        return (int)this.mode.choice();
    }
}
