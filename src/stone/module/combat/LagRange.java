package stone.module.combat;

import stone.event.AttackEntityEvent;
import stone.event.PacketSendEvent;
import stone.event.PrePlayerInteractEvent;
import stone.event.RenderWorldLastEvent;
import stone.event.Subscribe;
import stone.event.TickStartEvent;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.ModuleManager;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.player.BedNuker;
import stone.module.player.Blink;
import stone.module.player.FakeLag;
import stone.module.shared.DisabledOrNestedCondition;
import stone.module.shared.TargetFinder;
import stone.util.ClientUtils;
import stone.util.packet.PacketDirection;
import stone.util.packet.PacketListenerRegistration;
import stone.util.packet.QueuedPacketDispatcher;
import stone.util.render.FakePlayerRenderer;
import stone.util.render.FakePlayerRenderer$1;
import stone.util.render.FakePlayerRenderer$2;
import stone.util.rotation.RotationUtils;
import java.util.List;
import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import org.lwjgl.input.Mouse;

public class LagRange extends Module {
    private static final String[] ThT = new String[]{"Outbound", "Both", "Inbound"};
    private final Setting mode;
    private final Setting range;
    private final Setting maximumDelay;
    private final Setting hurtTime;
    private final Setting inboundRatio;
    private final Setting flushOnSprintReset;
    private final Setting flushOnBlock;
    private final Setting flushOnPotion;
    private final Setting holdingLeftClick;
    private final Setting holdingAWeapon;
    private final Setting lookingAtPlayer;
    private final Setting realPositionIndicator;
    private final Setting showInFirstPerson;
    private EntityPlayer entityPlayer;
    private boolean lagActive;
    private boolean PBo;
    private boolean wqB;
    private PacketListenerRegistration packetQueue;
    private final FakePlayerRenderer$1 snapshotHistory = new FakePlayerRenderer$1();

    public LagRange() {
        super("LagRange", "Flush conditions", Category.COMBAT, Subcategory.EXTREME);
        this.mode = addChoice("Mode", "", List.of(ThT), 0);
        this.range = addNumber("Range", "", 6.0F, 3.0F, 10.0F, 1);
        this.maximumDelay = addNumber("Maximum delay", "", 200.0F, 50.0F, 1000.0F, 0);
        this.hurtTime = addNumber("Hurt time", "", 150.0F, 0.0F, 500.0F, 0);
        this.inboundRatio = addNumber("Inbound ratio", "", 0.5F, 0.1F, 1.0F, 2);
        this.flushOnSprintReset = addBoolean("Flush on Sprint Reset", "", true);
        this.flushOnBlock = addBoolean("Flush on Block", "", true);
        this.flushOnPotion = addBoolean("Flush on Potion", "", true);
        this.realPositionIndicator = addBoolean("Real position indicator", "", true);
        this.showInFirstPerson = addBoolean("Show in first person", "", false);
        this.holdingLeftClick = addBoolean("Holding left click", "", true);
        this.holdingAWeapon = addBoolean("Holding a weapon", "", true);
        this.lookingAtPlayer = addBoolean("Looking at player", "", false);
    }

    @Override
    public void guiUpdate() {
        this.inboundRatio.setVisible(this.gNejbO9());
    }

    @Override
    public void onEnable(Context context) {
        this.resetState();
    }

    @Override
    public void onDisable(Context context) {
        this.YtpInez();
        this.resetState();
    }

    @Override
    public String suffix() {
        return (int)this.maximumDelay.number() + "ms";
    }

    @Subscribe
    public void onPrePlayerInteract(PrePlayerInteractEvent var1) {
        if (ClientUtils.isInWorld() && !mc.thePlayer.isDead && mc.theWorld != null) {
            if (this.isOtherLagModuleActive()) {
                if (this.lagActive) {
                    this.YtpInez();
                }

                this.resetState();
            } else if (ModuleManager.get(BedNuker.class) != null && ModuleManager.get(BedNuker.class).isNukingTarget()) {
                if (this.lagActive) {
                    this.YtpInez();
                }
            } else if (this.TOk1()) {
                if (this.lagActive) {
                    this.YtpInez();
                }
            } else if (this.isPlayerHurt()) {
                if (this.lagActive) {
                    this.YtpInez();
                }

                this.PBo = mc.thePlayer.isSprinting();
                this.wqB = mc.thePlayer.isBlocking();
            } else {
                double var2 = this.range.number() * this.range.number();
                boolean var4 = this.YTKG();
                boolean var5 = !this.holdingLeftClick.bool() || Mouse.isButtonDown(0);
                boolean var6 = !this.holdingAWeapon.bool() || ClientUtils.isHoldingWeapon();
                boolean var7 = this.lookingAtPlayer.bool();
                EntityPlayer var8 = this.findTargetPlayer(var2);
                this.entityPlayer = var8;
                if (this.entityPlayer == null) {
                    if (this.lagActive) {
                        this.YtpInez();
                    }
                } else {
                    double var9 = RotationUtils.getDistanceSqToEntity(this.entityPlayer);
                    boolean var11 = !var7 || this.isLookingAtPlayer(this.entityPlayer);
                    if (var9 > var2 || !var4 || !var5 || !var6 || !var11) {
                        if (this.lagActive) {
                            this.YtpInez();
                        }

                        this.PBo = mc.thePlayer.isSprinting();
                        this.wqB = mc.thePlayer.isBlocking();
                        return;
                    }

                    if (this.lagActive) {
                        if (this.isOutboundDelayEnabled()) {
                            QueuedPacketDispatcher.INSTANCE.Cvuz(PacketDirection.OUTBOUND, (long)this.maximumDelay.number());
                        }

                        if (this.isInboundDelayEnabled()) {
                            QueuedPacketDispatcher.INSTANCE.Cvuz(PacketDirection.INBOUND, this.getInboundDelayMillis());
                        }

                        if (this.flushOnSprintReset.bool()) {
                            boolean var12 = mc.thePlayer.isSprinting();
                            if (var12 && !this.PBo) {
                                this.YtpInez();
                                this.PBo = var12;
                                return;
                            }

                            this.PBo = var12;
                        }

                        if (this.flushOnBlock.bool()) {
                            boolean var13 = mc.thePlayer.isBlocking();
                            if (var13 && !this.wqB) {
                                this.YtpInez();
                                this.wqB = var13;
                                return;
                            }

                            this.wqB = var13;
                        }

                        if (this.flushOnPotion.bool() && mc.thePlayer.isUsingItem()) {
                            ItemStack var14 = mc.thePlayer.getHeldItem();
                            if (var14 != null && var14.getItem() instanceof ItemPotion && ItemPotion.isSplash(var14.getMetadata())) {
                                this.YtpInez();
                                return;
                            }
                        }

                        return;
                    }

                    this.PBo = mc.thePlayer.isSprinting();
                    this.wqB = mc.thePlayer.isBlocking();
                    this.startLag();
                }
            }
        } else {
            if (this.lagActive) {
                this.YtpInez();
            }

            this.resetState();
        }
    }

    @Subscribe
    public void onAttackEntity(AttackEntityEvent var1) {
        if (this.lagActive && var1.entityPlayer == mc.thePlayer && this.isWithinRange(var1.entity)) {
            this.YtpInez();
        }
    }

    @Subscribe(priority = Subscribe.Priority.HIGH)
    public void onPacketSend(PacketSendEvent var1) {
        if (this.lagActive) {
            Packet var2 = var1.getPacket();
            if (this.isAttackPacket(var2)) {
                C02PacketUseEntity var3 = (C02PacketUseEntity)var2;
                Entity var4 = mc.theWorld == null ? null : var3.getEntityFromWorld(mc.theWorld);
                if (this.isWithinRange(var4)) {
                    this.YtpInez();
                }
            } else {
                if (this.isActionPacket(var2)) {
                    this.YtpInez();
                }
            }
        }
    }

    @Subscribe
    public void onTickStart(TickStartEvent var1) {
        if (mc.currentScreen != null) {
            if (this.lagActive) {
                this.YtpInez();
            }

            this.resetState();
        }
    }

    @Subscribe
    public void onRenderWorldLast(RenderWorldLastEvent var1) {
        if (ClientUtils.isInWorld()) {
            if (this.lagActive) {
                if (this.realPositionIndicator.bool()) {
                    if (this.isOutboundDelayEnabled()) {
                        if (mc.gameSettings.thirdPersonView != 0 || this.showInFirstPerson.bool()) {
                            long var2 = (long)this.maximumDelay.number();
                            this.snapshotHistory.recordSnapshot(mc.thePlayer, var1.partialTicks);
                            FakePlayerRenderer$2 var4 = this.snapshotHistory.YJfvaQ(var2);
                            if (var4 != null) {
                                FakePlayerRenderer.renderFakePlayerWithDefaultAlpha(mc.thePlayer, var4, var1.partialTicks);
                            }
                        }
                    }
                }
            }
        }
    }

    private void startLag() {
        this.packetQueue = new PacketListenerRegistration(this.getPacketDirections(), new DisabledOrNestedCondition(this));
        QueuedPacketDispatcher.INSTANCE.JUlwlNu(this.packetQueue);
        this.lagActive = true;
    }

    private boolean gNejbO9() {
        return (int)this.mode.choice() == 1;
    }

    private boolean isInboundDelayEnabled() {
        int var1 = (int)this.mode.choice();
        return var1 == 1 || var1 == 2;
    }

    private boolean isOutboundDelayEnabled() {
        int var1 = (int)this.mode.choice();
        return var1 == 0 || var1 == 1;
    }

    private long getInboundDelayMillis() {
        double var1 = this.gNejbO9() ? this.inboundRatio.number() : 1.0;
        return (long)(this.maximumDelay.number() * var1);
    }

    private Set<PacketDirection> getPacketDirections() {
        switch ((int)this.mode.choice()) {
            case 0:
            default:
                return PacketDirection.ONLY_OUTBOUND;
            case 1:
                return PacketDirection.BIDIRECTIONAL;
            case 2:
                return PacketDirection.ONLY_INBOUND;
        }
    }

    private void YtpInez() {
        if (this.lagActive) {
            if (this.packetQueue != null) {
                this.packetQueue.getPacketHandler().forceOpen();
                this.packetQueue = null;
            }

            this.lagActive = false;
            this.snapshotHistory.clearHistory();
        }
    }

    private void resetState() {
        this.entityPlayer = null;
        this.lagActive = false;
        this.PBo = false;
        this.wqB = false;
        this.packetQueue = null;
        this.snapshotHistory.clearHistory();
    }

    private boolean isAttackPacket(Packet<?> var1) {
        return var1 instanceof C02PacketUseEntity && ((C02PacketUseEntity)var1).getAction() == Action.ATTACK;
    }

    private boolean isActionPacket(Packet<?> var1) {
        return var1 instanceof C02PacketUseEntity
            ? ((C02PacketUseEntity)var1).getAction() != Action.ATTACK
            : var1 instanceof C07PacketPlayerDigging || var1 instanceof C08PacketPlayerBlockPlacement;
    }

    private static int millisToTicks(double var0) {
        return var0 <= 0.0 ? 0 : (int)Math.ceil(var0 / 50.0);
    }

    private boolean isWithinRange(Entity var1) {
        double var2 = this.range.number() * this.range.number();
        return this.isEntityInRange(var1, var2) ? true : this.QbBkhe(var2);
    }

    private boolean QbBkhe(double var1) {
        if (mc.theWorld == null) {
            return false;
        } else {
            for (EntityPlayer var4 : mc.theWorld.playerEntities) {
                if (TargetFinder.eOkw(var4, var1) && this.passesLookingCondition(var4) && this.isHurtTimeElapsed(var4)) {
                    return true;
                }
            }

            return false;
        }
    }

    private boolean isEntityInRange(Entity var1, double var2) {
        if (!(var1 instanceof EntityPlayer)) {
            return false;
        } else {
            EntityPlayer var4 = (EntityPlayer)var1;
            return TargetFinder.eOkw(var4, var2) && this.passesLookingCondition(var4) && this.isHurtTimeElapsed(var4);
        }
    }

    private boolean isHurtTimeElapsed(EntityPlayer var1) {
        if (var1.hurtTime <= 0) {
            return true;
        } else {
            int var2 = var1.maxHurtTime > 0 ? var1.maxHurtTime : 10;
            int var3 = Math.max(0, var2 - var1.hurtTime);
            return var3 >= millisToTicks(this.hurtTime.number());
        }
    }

    private boolean TOk1() {
        AutoBlock var1 = ModuleManager.get(AutoBlock.class);
        return var1.isAutoBlockActive();
    }

    private boolean isOtherLagModuleActive() {
        return ModuleManager.get(Blink.class) != null && ModuleManager.get(Blink.class).shouldDisableLagrange()
            || ModuleManager.get(FakeLag.class) != null && ModuleManager.get(FakeLag.class).isLagrangeDisableActive();
    }

    private EntityPlayer findTargetPlayer(double var1) {
        if (ModuleManager.get(AimAssist.class) != null && ModuleManager.get(AimAssist.class).enabled()) {
            EntityPlayer var3 = ModuleManager.get(AimAssist.class).getTargetPlayer();
            if (TargetFinder.eOkw(var3, var1) && this.passesLookingCondition(var3)) {
                return var3;
            }
        }

        return !this.lookingAtPlayer.bool() ? TargetFinder.findNearestTarget(var1) : this.findLookedAtPlayer(var1);
    }

    private EntityPlayer findLookedAtPlayer(double var1) {
        if (mc.theWorld == null) {
            return null;
        } else {
            EntityPlayer var3 = null;
            double var4 = Double.MAX_VALUE;

            for (EntityPlayer var7 : mc.theWorld.playerEntities) {
                if (TargetFinder.eOkw(var7, var1) && this.isLookingAtPlayer(var7)) {
                    double var8 = RotationUtils.getDistanceSqToEntity(var7);
                    if (var8 < var4) {
                        var4 = var8;
                        var3 = var7;
                    }
                }
            }

            return var3;
        }
    }

    private boolean YTKG() {
        return mc.thePlayer.moveForward != 0.0F || mc.thePlayer.moveStrafing != 0.0F;
    }

    private boolean isPlayerHurt() {
        return mc.thePlayer.hurtTime > 0;
    }

    private boolean isLookingAtPlayer(EntityPlayer var1) {
        return var1 != null && ClientUtils.Bhr95(mc.thePlayer.rotationYaw, mc.gameSettings.fovSetting, RotationUtils.ilaZ(var1.posX, var1.posZ));
    }

    private boolean passesLookingCondition(EntityPlayer var1) {
        return !this.lookingAtPlayer.bool() || this.isLookingAtPlayer(var1);
    }

    public boolean isLagging() {
        return this.enabled() && this.lagActive;
    }

}
