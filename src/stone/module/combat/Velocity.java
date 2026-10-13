package stone.module.combat;

import stone.event.PacketReceiveEvent;
import stone.event.PostWalkingUpdateEvent;
import stone.event.PreUpdateEvent;
import stone.event.Subscribe;
import stone.event.TickStartEvent;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.ModuleManager;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.combat.velocity.TimeoutCondition;
import stone.module.combat.velocity.VelocityMode;
import stone.module.movement.Stasis;
import stone.module.player.Blink;
import stone.module.player.Buffer;
import stone.module.shared.DisabledOrNestedCondition;
import stone.module.shared.TargetFinder;
import stone.ui.Client;
import stone.util.ClientUtils;
import stone.util.item.ItemList;
import stone.util.packet.PacketDirection;
import stone.util.packet.PacketListenerRegistration;
import stone.util.packet.QueuedPacketDispatcher;
import stone.util.rotation.RotationUtils;
import java.util.List;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class Velocity extends Module {
    private final Setting mode;
    private final Setting resetChance;
    private final Setting delayChance;
    private final Setting distanceToTarget;
    private final Setting maximumDelay;
    private final Setting requireMouseDown;
    private final Setting requireMovingForward;
    private final Setting requireAim;
    private final Setting inAir;
    private final Setting lookingAtPlayer;
    private final Setting requireLeftMouse;
    private final Setting restrictHeldItem;
    private final Setting delay;
    private final ItemList whitelistedItems;
    private PacketListenerRegistration TWo;
    private long inboundDelayMillis;
    private boolean delayApplied;
    private boolean jumpKeyHeld;
    private boolean zGpfe;
    private boolean ignoreFallDamage;
    private long fallDamageIgnoreUntil;
    private boolean resetAlreadyTriggered;
    private int previousHurtTime;
    private double previousFallDistance;
    private double maximumFallDistance;
    private boolean Nfas;
    private long resetDueAtMillis;
    private long EPK;
    private double pendingKnockbackSpeed;
    private long forwardHoldUntilMillis;

    public Velocity() {
        super("Velocity", "Reset Conditions", Category.COMBAT, Subcategory.EXTREME);
        this.mode = addChoice("Mode", "", List.of(VelocityMode.labels()), VelocityMode.JUMP_RESET.ordinal());
        this.resetChance = addNumber("Reset Chance", "", 100.0F, 0.0F, 100.0F, 0);
        this.delayChance = addNumber("Delay Chance", "", 100.0F, 0.0F, 100.0F, 0);
        this.distanceToTarget = addNumber("Distance to target", "", 5.3F, 3.0F, 12.0F, 1);
        this.maximumDelay = addNumber("Maximum delay", "", 200.0F, 50.0F, 1000.0F, 0);
        this.requireMouseDown = addBoolean("Require mouse down", "", false);
        this.requireMovingForward = addBoolean("Require moving forward", "", false);
        this.requireAim = addBoolean("Require aim", "", false);
        this.delay = addBoolean("Delay", "", true);
        this.inAir = addBoolean("In air", "", true);
        this.lookingAtPlayer = addBoolean("Looking at player", "", true);
        this.requireLeftMouse = addBoolean("Require Left mouse", "", true);
        this.restrictHeldItem = addBoolean("Restrict held item", "", false);
        this.whitelistedItems = new ItemList();
        this.DcLu8();
    }

    private void DcLu8() {
        this.delayChance.setVisible(false);
        this.distanceToTarget.setVisible(false);
        this.maximumDelay.setVisible(false);
        this.delay.setVisible(false);
        this.inAir.setVisible(false);
        this.lookingAtPlayer.setVisible(false);
        this.requireLeftMouse.setVisible(false);
        this.restrictHeldItem.setVisible(false);
    }

    @Override
    public void guiUpdate() {
        boolean var1 = this.ymnN();
        boolean var2 = this.isHardResetMode();
        boolean var3 = var2 && this.delay.bool();
        this.delayChance.setVisible(var1 || var3);
        this.distanceToTarget.setVisible(var1 || var3);
        this.maximumDelay.setVisible(var1 || var3);
        this.delay.setVisible(var2);
        this.inAir.setVisible(var1 || var3);
        this.lookingAtPlayer.setVisible(var1 || var3);
        this.requireLeftMouse.setVisible(var1 || var3);
        this.restrictHeldItem.setVisible(var1 || var3);
    }

    @Override
    protected void settingChanged(Setting var1) {
        if (var1 == this.mode || var1 == this.delay && !this.delay.bool()) {
            this.clearPendingReset();
            this.cancelInboundDelay();
        }
    }

    @Override
    public void onEnable(Context context) {
        if ((this.ymnN() || this.isHardResetMode()) && isBlinkConflict()) {
            ClientUtils.sendColoredMessage("&cVelocity conflicts with Blink inbound / both. Disable Blink or use outbound-only.");
            setEnabled(false, Client.context());
        } else {
            this.TWo = null;
            this.inboundDelayMillis = 0L;
            this.delayApplied = false;
        }
    }

    @Override
    public void onDisable(Context context) {
        this.clearPendingReset();
        this.cancelInboundDelay();
        if (this.jumpKeyHeld && !ClientUtils.isJumpKeyDown()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), this.jumpKeyHeld = false);
        }

        if (this.zGpfe && !Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), this.zGpfe = false);
        }

        this.jumpKeyHeld = false;
        this.zGpfe = false;
        this.ignoreFallDamage = false;
        this.fallDamageIgnoreUntil = 0L;
        this.maximumFallDistance = 0.0;
        this.resetAlreadyTriggered = false;
        this.forwardHoldUntilMillis = 0L;
    }

    @Override
    public String suffix() {
        VelocityMode var1 = this.getVelocityMode();
        if (var1 == VelocityMode.DELAY) {
            return (int)this.maximumDelay.number() + "ms";
        } else if (var1 == VelocityMode.HARD_RESET) {
            return "Hard";
        } else {
            return (int)this.resetChance.number() == 100 ? "" : (int)this.resetChance.number() + "%";
        }
    }

    @Subscribe
    public void onPreUpdate(PreUpdateEvent var1) {
        int var2 = mc.thePlayer.hurtTime;
        boolean var3 = mc.thePlayer.onGround;
        this.tickPendingReset();
        this.updateFallDamageTracking(var3);
        if ((this.isJumpResetMode() || this.ymnN()) && var2 > this.previousHurtTime) {
            if (this.resetAlreadyTriggered) {
                this.resetAlreadyTriggered = false;
            } else if (this.areResetConditionsMet(var3, false, false)) {
                this.clearPendingReset();
                if (this.ymnN()) {
                    this.HmxO5();
                }

                this.pressJumpKey();
            }

            this.clearFallTracking();
        }

        this.previousHurtTime = var2;
        this.previousFallDistance = mc.thePlayer.fallDistance;
    }

    @Subscribe(priority = Subscribe.Priority.HIGHEST)
    public void onPacketReceive(PacketReceiveEvent var1) {
        if (this.enabled() && !var1.cancelled() && (this.ymnN() || this.isHardResetMode())) {
            if (var1.getPacket() instanceof S08PacketPlayerPosLook) {
                this.clearPendingReset();
                this.cancelInboundDelay();
            } else if (this.isOwnHurtPacket(var1.getPacket()) && this.deepod()) {
                QueuedPacketDispatcher.INSTANCE.markHandled(var1.getPacket());
            } else if (var1.getPacket() instanceof S12PacketEntityVelocity) {
                if (ClientUtils.isInWorld() && mc.thePlayer != null && mc.theWorld != null) {
                    S12PacketEntityVelocity var2 = (S12PacketEntityVelocity)var1.getPacket();
                    if (var2.getEntityID() == mc.thePlayer.getEntityId()) {
                        if (!this.isPacketHandlingConflict()) {
                            if (this.isHardResetMode()) {
                                this.handleVelocityPacket(var2);
                            } else if (!this.deepod()) {
                                this.delayApplied = this.AItYgn() == null && (this.delayChance.number() >= 100.0 || Math.random() * 100.0 < this.delayChance.number());
                                if (this.delayApplied) {
                                    this.inboundDelayMillis = this.getConfiguredMaximumDelay();
                                    this.scheduleInboundDelay(this.inboundDelayMillis + 50L);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Subscribe(priority = Subscribe.Priority.LOWEST)
    public void onTickStart(TickStartEvent var1) {
        if (this.enabled() && (this.ymnN() || this.isHardResetMode())) {
            if (!ClientUtils.isInWorld() || mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.isDead) {
                this.clearPendingReset();
                this.cancelInboundDelay();
            } else if (!this.deepod()) {
                this.clearFinishedDelay();
            } else if (this.isHardResetMode() && this.getTargetFailureReason() != null) {
                this.clearPendingReset();
                this.cancelInboundDelay();
            } else if ((this.ymnN() || this.isHardResetMode()) && this.delayApplied && this.AItYgn() != null) {
                if (this.isHardResetMode()) {
                    this.clearPendingReset();
                }

                this.cancelInboundDelay();
            } else {
                QueuedPacketDispatcher.INSTANCE.Cvuz(PacketDirection.INBOUND, this.inboundDelayMillis);
            }
        }
    }

    @Subscribe
    public void BPry(PostWalkingUpdateEvent var1) {
        if (this.jumpKeyHeld && !ClientUtils.isJumpKeyDown()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), this.jumpKeyHeld = false);
        }

        if (this.zGpfe && !Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()) && System.currentTimeMillis() >= this.forwardHoldUntilMillis) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), this.zGpfe = false);
        }
    }

    private void handleVelocityPacket(S12PacketEntityVelocity var1) {
        String var2 = this.getResetFailureReason(mc.thePlayer.onGround, true, true, ClientUtils.randomDouble(0.0, 100.0), true);
        if (var2 == null) {
            if (this.getTargetFailureReason() == null) {
                if (!this.delay.bool()) {
                    this.scheduleKnockbackReset(this.getKnockbackSpeed(var1), 0L);
                    this.resetAlreadyTriggered = true;
                } else {
                    if (!this.deepod()) {
                        this.delayApplied = this.AItYgn() == null && (this.delayChance.number() >= 100.0 || Math.random() * 100.0 < this.delayChance.number());
                        this.inboundDelayMillis = this.delayApplied ? this.getConfiguredMaximumDelay() : 50L;
                        this.scheduleInboundDelay(this.inboundDelayMillis + 50L);
                    }

                    this.euuY(var1);
                    this.resetAlreadyTriggered = true;
                }
            }
        }
    }

    private boolean areResetConditionsMet(boolean var1, boolean var2, boolean var3) {
        return this.getResetFailureReason(var1, var2, var3, ClientUtils.randomDouble(0.0, 100.0), true) == null;
    }

    private String getResetFailureReason(boolean var1, boolean var2, boolean var3, double var4, boolean var6) {
        boolean var7 = mc.gameSettings.keyBindAttack.isKeyDown() || !this.requireMouseDown.bool();
        boolean var8 = !this.requireAim.bool() || this.isAimingAtPlayer();
        boolean var9 = mc.gameSettings.keyBindForward.isKeyDown() || !this.requireMovingForward.bool() || var3;
        boolean var10 = (int)this.resetChance.number() == 100 || var4 < this.resetChance.number();
        boolean var11 = !this.requireAim.bool() || ClientUtils.Bhr95(ClientUtils.computeInputMovementAngle(), 330.0F, RotationUtils.computeYawFromDelta(mc.thePlayer.motionX, mc.thePlayer.motionZ));
        boolean var12 = mc.thePlayer.isBurning();
        boolean var13 = this.AuVw();
        if (var6 && this.isFallDamageIgnored(var1)) {
            return "fall-damage ignore flag is active";
        } else if (var12) {
            return "player is burning";
        } else if (!var1 && !var2) {
            return "player is not on ground";
        } else if (!var8) {
            return "aim requirement failed";
        } else if (!var9) {
            return "forward requirement failed";
        } else if (!var7) {
            return "mouse-down requirement failed";
        } else if (!var10) {
            return "chance failed, rolled " + this.DTcxR(var4) + " against " + this.DTcxR(this.resetChance.number());
        } else if (var13) {
            return "jump, poison, or wither potion effect is active";
        } else {
            return !var11 ? "knockback direction is outside required FOV" : null;
        }
    }

    private void updateFallDamageTracking(boolean var1) {
        if (mc.thePlayer.capabilities.allowFlying) {
            this.clearFallTracking();
        } else {
            double var2 = mc.thePlayer.fallDistance;
            if (var2 > this.maximumFallDistance) {
                this.maximumFallDistance = var2;
            }

            if (var1 && Math.max(this.previousFallDistance, this.maximumFallDistance) > 3.0) {
                this.ignoreFallDamage = true;
                this.fallDamageIgnoreUntil = System.currentTimeMillis() + 750L;
                this.maximumFallDistance = 0.0;
            }

            if (this.ignoreFallDamage && System.currentTimeMillis() > this.fallDamageIgnoreUntil) {
                this.clearFallTracking();
            }

            if (var1 && !this.ignoreFallDamage) {
                this.maximumFallDistance = 0.0;
            }
        }
    }

    private boolean isFallDamageIgnored(boolean var1) {
        return this.ignoreFallDamage && System.currentTimeMillis() <= this.fallDamageIgnoreUntil
            ? true
            : var1 && !mc.thePlayer.capabilities.allowFlying && (this.previousFallDistance > 3.0 || this.maximumFallDistance > 3.0);
    }

    private void clearFallTracking() {
        this.ignoreFallDamage = false;
        this.fallDamageIgnoreUntil = 0L;
        this.maximumFallDistance = 0.0;
    }

    private String AItYgn() {
        String var1 = this.getTargetFailureReason();
        if (var1 != null) {
            return var1;
        } else {
            double var2 = this.distanceToTarget.number() * this.distanceToTarget.number();
            if (this.inAir.bool() && mc.thePlayer.onGround) {
                return "not in air";
            } else if (this.lookingAtPlayer.bool() && TargetFinder.findCrosshairTarget(var2) == null) {
                return "not looking at player";
            } else if (this.requireLeftMouse.bool() && !Mouse.isButtonDown(0)) {
                return "LMB not held";
            } else {
                if (this.restrictHeldItem.bool()) {
                    ItemStack var4 = mc.thePlayer.getHeldItem();
                    if (var4 == null || !this.whitelistedItems.matches(var4)) {
                        return "held item not whitelisted";
                    }
                }

                return null;
            }
        }
    }

    private String getTargetFailureReason() {
        double var1 = this.distanceToTarget.number() * this.distanceToTarget.number();
        return TargetFinder.findTarget(var1) == null ? "no target in range" : null;
    }

    private void pressJumpKey() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), this.jumpKeyHeld = true);
    }

    private void Mpih(double var1, long var3) {
        long var5 = this.getForwardHoldMillis(var1);
        if (var5 <= 0L) {
            if (this.zGpfe && !Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), this.zGpfe = false);
            }
        } else {
            this.forwardHoldUntilMillis = Math.max(this.forwardHoldUntilMillis, System.currentTimeMillis() + var3 + var5);
            if (!Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
                this.zGpfe = true;
            }
        }
    }

    private void HmxO5() {
        long var1 = 90L;
        this.forwardHoldUntilMillis = Math.max(this.forwardHoldUntilMillis, System.currentTimeMillis() + var1);
        if (!Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
            this.zGpfe = true;
        }
    }

    private void euuY(S12PacketEntityVelocity var1) {
        this.applyPendingKnockback(this.getKnockbackSpeed(var1));
    }

    private void applyPendingKnockback(double var1) {
        this.scheduleKnockbackReset(var1, this.getPendingResetDelayMillis());
    }

    private void scheduleKnockbackReset(double var1, long var3) {
        this.Nfas = true;
        this.EPK = System.currentTimeMillis();
        this.resetDueAtMillis = this.EPK + Math.max(0L, var3);
        this.pendingKnockbackSpeed = var1;
    }

    private long getPendingResetDelayMillis() {
        return Math.max(0L, this.getConfiguredMaximumDelay() - 50L);
    }

    private void tickPendingReset() {
        if (this.Nfas && System.currentTimeMillis() >= this.resetDueAtMillis) {
            if (this.canResetNow()) {
                this.pressJumpKey();
                this.Mpih(this.pendingKnockbackSpeed, 0L);
            }

            this.clearPendingReset();
        }
    }

    private boolean canResetNow() {
        return this.getTargetFailureReason() == null && !mc.thePlayer.isBurning() && !this.AuVw();
    }

    private void clearPendingReset() {
        this.Nfas = false;
        this.resetDueAtMillis = 0L;
        this.EPK = 0L;
        this.pendingKnockbackSpeed = 0.0;
    }

    private double getKnockbackSpeed(S12PacketEntityVelocity var1) {
        double var2 = var1.getMotionX() / 8000.0;
        double var4 = var1.getMotionZ() / 8000.0;
        return Math.sqrt(var2 * var2 + var4 * var4);
    }

    private long getForwardHoldMillis(double var1) {
        if (var1 <= 0.45) {
            return 180L;
        } else if (var1 >= 0.75) {
            return 360L;
        } else {
            double var3 = (var1 - 0.45) / 0.3;
            return 180L + Math.round(180.0 * var3);
        }
    }

    private boolean AuVw() {
        PotionEffect var1 = mc.thePlayer.getActivePotionEffect(Potion.jump);
        PotionEffect var2 = mc.thePlayer.getActivePotionEffect(Potion.poison);
        PotionEffect var3 = mc.thePlayer.getActivePotionEffect(Potion.wither);
        return var1 != null || var2 != null || var3 != null;
    }

    private boolean isAimingAtPlayer() {
        MovingObjectPosition var1 = mc.objectMouseOver;
        return var1 != null && var1.typeOfHit == MovingObjectType.ENTITY && var1.entityHit instanceof EntityPlayer;
    }

    private boolean isOwnHurtPacket(Object var1) {
        if (var1 instanceof S19PacketEntityStatus && mc.theWorld != null && mc.thePlayer != null) {
            S19PacketEntityStatus var2 = (S19PacketEntityStatus)var1;
            return var2.getOpCode() == 2 && var2.getEntity(mc.theWorld) == mc.thePlayer;
        } else {
            return false;
        }
    }

    private boolean deepod() {
        return this.TWo != null && !this.TWo.getPacketHandler().isOpen();
    }

    public boolean isInboundDelaying() {
        return this.enabled() && this.deepod();
    }

    public long getConfiguredMaximumDelay() {
        return (long)this.maximumDelay.number();
    }

    public boolean isHardResetEnabled() {
        return this.enabled() && this.isHardResetMode();
    }

    private boolean isPacketHandlingConflict() {
        Buffer var1 = ModuleManager.get(Buffer.class);
        if (var1.isBufferEnabled()) {
            return true;
        } else {
            Stasis var2 = ModuleManager.get(Stasis.class);
            return var2.isHypixelModeActive();
        }
    }

    private void scheduleInboundDelay(long var1) {
        this.TWo = new PacketListenerRegistration(PacketDirection.ONLY_INBOUND, new DisabledOrNestedCondition(this, new TimeoutCondition(var1)));
        QueuedPacketDispatcher.INSTANCE.JUlwlNu(this.TWo);
    }

    private void clearFinishedDelay() {
        if (this.TWo != null && this.TWo.getPacketHandler().isOpen()) {
            this.TWo = null;
            this.inboundDelayMillis = 0L;
            this.delayApplied = false;
        }
    }

    private void cancelInboundDelay() {
        if (this.TWo != null) {
            this.TWo.getPacketHandler().forceOpen();
            this.TWo = null;
        }

        this.inboundDelayMillis = 0L;
        this.delayApplied = false;
    }

    private boolean isJumpResetMode() {
        return this.getVelocityMode() == VelocityMode.JUMP_RESET;
    }

    private boolean ymnN() {
        return this.getVelocityMode() == VelocityMode.DELAY;
    }

    private boolean isHardResetMode() {
        return this.getVelocityMode() == VelocityMode.HARD_RESET;
    }

    private VelocityMode getVelocityMode() {
        return VelocityMode.fromSetting(this.mode.choice());
    }

    private String DTcxR(double var1) {
        return Math.round(var1) + "%";
    }

    private static boolean isBlinkConflict() {
        Blink var0 = ModuleManager.get(Blink.class);
        return var0.enabled() && var0.isInboundMode();
    }

}
