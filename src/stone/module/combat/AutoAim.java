package stone.module.combat;

import stone.event.DisconnectEvent;
import stone.event.LoadWorldEvent;
import stone.event.PacketReceiveEvent;
import stone.event.PreUpdateEvent;
import stone.event.RenderWorldLastEvent;
import stone.event.RotationEvent;
import stone.event.Subscribe;
import stone.event.TickStartEvent;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.combat.autoaim.AimResult;
import stone.module.combat.autoaim.AimResult$0;
import stone.module.combat.autoaim.AimSolver;
import stone.module.combat.autoaim.AimSolver$2;
import stone.module.combat.autoaim.EntityPositionTracker;
import stone.module.combat.autoaim.MotionPredictor;
import stone.module.combat.autoaim.MotionPredictor$1;
import stone.module.combat.autoaim.TargetPrediction;
import stone.module.other.AntiBot;
import stone.module.shared.ProjectileMotion;
import stone.ui.Theme;
import stone.util.ClientUtils;
import stone.util.render.RenderUtils;
import stone.util.rotation.RotationHandler;
import stone.util.rotation.RotationUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public final class AutoAim extends Module {
    private final Setting bow;
    private final Setting rod;
    private final Setting fov;
    private final Setting range;
    private final Setting players;
    private final Setting teammates;
    private final Setting invisible;
    private final Setting bowFlight;
    private final Setting minimumConfidence;
    private final Setting rodMode;
    private final Setting minimumRodRange;
    private final Setting maximumRodRange;
    private final Setting cooldown;
    private final Setting notWhilstHoldingLeft;
    private final Setting notWhilstHoldingRight;
    private final Setting holdingWeapon;
    private final Setting opponentLooking;
    private final Setting visualise;
    private final MotionPredictor motionPredictor = new MotionPredictor();
    private final EntityPositionTracker entityPositionTracker = new EntityPositionTracker();
    private final AimSolver aimSolver = new AimSolver(this.motionPredictor);
    private final Set<Integer> BXFwSu = new HashSet<>();
    private final Set<Integer> TcE = new HashSet<>();
    private final Map<Integer, EntityPlayer> trackedPlayersById = new HashMap<>();
    private EntityPlayer entityPlayer;
    private AimResult aimSolution;
    private AutoAim$1 pendingAction = AutoAim$1.NONE;
    private AutoAim$2 rodState = AutoAim$2.READY;
    private int hitStreak;
    private int cooldownTicks;
    private int castStartTick;
    private int missStreak;
    private double smoothedLatencyMillis;
    private boolean LvifHr;
    private boolean swappedToRod;
    private int Vx3 = -1;
    private int visualisedEntityId = -1;
    private long lastVisualisationNanos;
    private double smoothedEntityX;
    private double smoothedEntityY;
    private double smoothedEntityZ;
    private double FfVm;
    private double smoothedAimY;
    private double smoothedAimZ;
    private boolean aimIndicatorInitialised;
    private boolean srOx1;
    private float smoothedYaw;
    private float LjSl;

    public AutoAim() {
        super("AutoAim", "Aims bows and rods where the target will be", Category.COMBAT, Subcategory.ADVANCED);
        this.bow = addBoolean("Bow", "", true);
        this.rod = addBoolean("Rod", "", true);
        this.fov = addNumber("FOV", "", 8.0F, 1.0F, 360.0F, 0);
        this.range = addNumber("Range", "", 80.0F, 8.0F, 160.0F, 0);
        this.players = addBoolean("Players", "", true);
        this.teammates = addBoolean("Teammates", "", false);
        this.invisible = addBoolean("Invisible", "", false);
        this.bowFlight = addNumber("Bow Flight", "", 1.5F, 0.2F, 3.5F, 1);
        this.minimumConfidence = addNumber("Minimum Confidence", "", 40.0F, 20.0F, 95.0F, 0);
        this.rodMode = addChoice("Rod Mode", "", List.of("Manual", "Automatic"), 0);
        this.minimumRodRange = addNumber("Minimum Rod Range", "", 2.8F, 1.0F, 10.0F, 1);
        this.maximumRodRange = addNumber("Maximum Rod Range", "", 4.5F, 1.0F, 12.0F, 1);
        this.cooldown = addNumber("Cooldown", "", 0.5F, 0.05F, 1.5F, 2);
        this.notWhilstHoldingLeft = addBoolean("Not whilst holding left", "", true);
        this.notWhilstHoldingRight = addBoolean("Not whilst holding right", "", true);
        this.holdingWeapon = addBoolean("Holding weapon", "", true);
        this.opponentLooking = addBoolean("Opponent looking", "", true);
        this.visualise = addBoolean("Visualise", "", false);
    }

    @Override
    public String suffix() {
        return "";
    }

    @Override
    public void guiUpdate() {
        boolean var1 = this.rod.bool() && this.PMaFht();
        this.minimumRodRange.setVisible(var1);
        this.maximumRodRange.setVisible(var1);
        this.notWhilstHoldingLeft.setVisible(var1);
        this.notWhilstHoldingRight.setVisible(var1);
        this.holdingWeapon.setVisible(var1);
        this.opponentLooking.setVisible(var1);
    }

    @Override
    public void onEnable(Context context) {
        this.resetModuleState();
    }

    @Override
    public void onDisable(Context context) {
        this.resetModuleState();
    }

    @Subscribe(priority = Subscribe.Priority.HIGHEST)
    public void onPacketReceive(PacketReceiveEvent var1) {
        if (!var1.cancelled()) {
            this.entityPositionTracker.onPacketReceived(var1.getPacket(), mc.theWorld);
        }
    }

    @Subscribe
    public void onLoadWorld(LoadWorldEvent var1) {
        this.resetModuleState();
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent var1) {
        this.resetModuleState();
    }

    @Subscribe
    public void onTickStart(TickStartEvent var1) {
        if (!ClientUtils.isInWorld()) {
            this.resetCombatState();
        } else {
            this.entityPositionTracker.flushToPredictor(this.motionPredictor);
            this.motionPredictor.setCurrentTime(System.nanoTime() / 5.0E7);
            this.purgeRemovedPlayers();
            this.updatePlayerTracking();
            if (this.cooldownTicks > 0) {
                this.cooldownTicks--;
                if (this.cooldownTicks == 0) {
                    this.rodState = AutoAim$2.READY;
                }
            }

            this.tickFishingRod();
        }
    }

    @Subscribe(priority = Subscribe.Priority.HIGH)
    public void onRotation(RotationEvent var1) {
        if (!this.canOperate()) {
            if (mc.thePlayer == null || mc.thePlayer.fishEntity == null) {
                this.restoreHotbarSlot();
            }

            this.clearAimSolution(false);
            this.clearRotationSmoothing();
        } else {
            boolean var2 = this.isUsingBow();
            boolean var3 = var2 && this.isBowFullyDrawn();
            boolean var4 = this.PMaFht() && this.rod.bool() && !var2;
            boolean var5 = !this.PMaFht() && this.rod.bool() && this.isHoldingRod();
            if (!var2 && !var5 && !var4) {
                this.clearAimSolution(true);
                this.clearRotationSmoothing();
            } else if (var4 && !this.areAutomaticConditionsMet()) {
                if (this.swappedToRod) {
                    this.restoreHotbarSlot();
                }

                this.clearAimSolution(true);
            } else {
                EntityPlayer var6 = this.entityPlayer;
                this.entityPlayer = var4 ? this.selectRodTarget(this.entityPlayer) : this.DEhn(this.entityPlayer);
                if (this.entityPlayer != var6) {
                    this.aimSolution = null;
                    this.hitStreak = 0;
                    this.pendingAction = AutoAim$1.NONE;
                    this.resetAimVisualisation();
                }

                if (this.entityPlayer == null) {
                    if (this.swappedToRod && mc.thePlayer.fishEntity == null) {
                        this.restoreHotbarSlot();
                    }

                    this.aimSolution = null;
                    this.hitStreak = 0;
                    if (var2) {
                        this.driftTowardPlayerAngles(var1);
                    }
                } else {
                    if (var4 && this.rodState == AutoAim$2.READY && mc.thePlayer.fishEntity == null && !this.isHoldingRod()) {
                        int var7 = this.findHotbarSlot(Items.fishing_rod);
                        int var8 = this.findSwordHotbarSlot();
                        if (var7 < 0 || var8 < 0) {
                            this.clearAimSolution(true);
                            return;
                        }

                        this.Vx3 = var8;
                    }

                    if (var4 && this.isHoldingRod() && !this.swappedToRod) {
                        this.Vx3 = this.findSwordHotbarSlot();
                        if (this.Vx3 < 0) {
                            return;
                        }

                        this.swappedToRod = true;
                    }

                    double var16 = this.minimumConfidence.number() / 100.0;
                    double var9 = this.smoothedLatencyMillis / 100.0;
                    if (var2) {
                        boolean var11 = this.motionPredictor.hasRecentVelocity(this.entityPlayer.getEntityId());
                        if (var11) {
                            this.aimSolution = null;
                            this.hitStreak = 0;
                            this.pendingAction = AutoAim$1.NONE;
                            this.applySmoothedRotation(var1);
                        } else {
                            double var12 = this.getBowDrawProgress();
                            double var14 = var9 + this.getLateralLeadCorrection(this.entityPlayer);
                            this.aimSolution = this.aimSolver.UMZR(mc.theWorld, mc.thePlayer, this.entityPlayer, AimSolver$2.BOW, 3.0, var14, var16);
                            this.updateAimOutcome(this.aimSolution);
                            if (this.isConfirmedHit(this.aimSolution)
                                && this.isTargetReachable(this.aimSolution)
                                && var3
                                && var12 >= 1.0
                                && this.aimSolution.getTicks() <= this.getBowFlightTicks()
                                && this.isWithinAngleTolerance(this.aimSolution, 1.5, 1.5)
                                && this.hitStreak >= 2) {
                                this.pendingAction = AutoAim$1.RELEASE_BOW;
                            }
                        }
                    } else if (this.rodState == AutoAim$2.READY && mc.thePlayer.fishEntity == null) {
                        this.aimSolution = this.aimSolver.UMZR(mc.theWorld, mc.thePlayer, this.entityPlayer, AimSolver$2.FISHING_ROD, 1.5, var9, var16);
                        this.updateAimOutcome(this.aimSolution);
                        if (this.isConfirmedHit(this.aimSolution) && this.isWithinAngleTolerance(this.aimSolution, 3.0, 3.0) && this.hitStreak >= 2) {
                            this.pendingAction = var4 && !this.isHoldingRod() ? AutoAim$1.SWAP_TO_ROD : AutoAim$1.CAST_ROD;
                        }
                    }

                    boolean var17 = !var2 || this.aimSolution != null && this.aimSolution.getTicks() <= this.getBowFlightTicks() && this.isTargetReachable(this.aimSolution);
                    if (this.isViableSolution(this.aimSolution) && var17) {
                        if (var2) {
                            this.smoothTowardTargetAngles(var1, this.aimSolution.getYaw(), this.aimSolution.getPitch());
                        } else {
                            RotationHandler.getInstance().ljYma8(false);
                            var1.setRotation(this.aimSolution.getYaw(), this.aimSolution.getPitch(), 55);
                        }
                    } else if (var2) {
                        this.applySmoothedRotation(var1);
                    }
                }
            }
        }
    }

    @Subscribe(priority = Subscribe.Priority.LOW)
    public void onPreUpdate(PreUpdateEvent var1) {
        if (this.pendingAction != AutoAim$1.NONE && this.canOperate()) {
            AutoAim$1 var2 = this.pendingAction;
            this.pendingAction = AutoAim$1.NONE;
            if (var2 == AutoAim$1.RELEASE_BOW && this.isUsingBow() && this.isBowFullyDrawn() && this.entityPlayer != null && !this.entityPositionTracker.hasPendingTeleport(this.entityPlayer.getEntityId())) {
                mc.playerController.onStoppedUsingItem(mc.thePlayer);
                if (Mouse.isButtonDown(1)) {
                    mc.rightClickDelayTimer = 0;
                }

                this.hitStreak = 0;
            } else if (var2 == AutoAim$1.SWAP_TO_ROD && this.Vx3 >= 0) {
                int var5 = this.findHotbarSlot(Items.fishing_rod);
                if (var5 >= 0) {
                    this.swappedToRod = true;
                    this.selectHotbarSlot(var5);
                    ItemStack var6 = mc.thePlayer.getHeldItem();
                    if (var6 != null && var6.getItem() == Items.fishing_rod && mc.thePlayer.fishEntity == null) {
                        mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, var6);
                        this.rodState = AutoAim$2.CAST;
                        this.castStartTick = mc.thePlayer.ticksExisted;
                        this.LvifHr = true;
                        this.hitStreak = 0;
                    }
                }
            } else if (this.ZRqx(var2) && this.isHoldingRod()) {
                ItemStack var3 = mc.thePlayer.getHeldItem();
                boolean var4 = mc.thePlayer.fishEntity != null;
                if (var3 != null) {
                    if (var2 == AutoAim$1.CAST_ROD && !var4) {
                        mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, var3);
                        this.rodState = AutoAim$2.CAST;
                        this.castStartTick = mc.thePlayer.ticksExisted;
                        this.LvifHr = true;
                    } else if (var2 == AutoAim$1.RECYCLE_ROD) {
                        this.LvifHr = false;
                        this.rodState = AutoAim$2.READY;
                        this.cooldownTicks = 0;
                        this.aimSolution = null;
                        if (this.swappedToRod) {
                            this.restoreHotbarSlot();
                        } else if (var4) {
                            mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, var3);
                        }
                    } else if (var2 == AutoAim$1.REEL_ROD_HIT || var2 == AutoAim$1.REEL_ROD_MISS) {
                        this.LvifHr = false;
                        if (this.swappedToRod) {
                            this.beginRodCooldown();
                            this.restoreHotbarSlot();
                        } else {
                            if (var4) {
                                mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, var3);
                            }

                            this.beginRodCooldown();
                        }
                    }

                    this.hitStreak = 0;
                }
            }
        }
    }

    private void tickFishingRod() {
        if (!this.rod.bool()) {
            this.restoreHotbarSlot();
            this.rodState = AutoAim$2.READY;
        } else if (!this.isHoldingRod()) {
            if (this.rodState != AutoAim$2.COOLDOWN && (!this.swappedToRod || this.pendingAction != AutoAim$1.SWAP_TO_ROD)) {
                this.swappedToRod = false;
                this.Vx3 = -1;
                this.rodState = AutoAim$2.READY;
            }
        } else {
            EntityFishHook var1 = mc.thePlayer.fishEntity;
            if (var1 == null) {
                int var8 = 10 + (int)Math.ceil(this.smoothedLatencyMillis / 50.0);
                if (this.rodState == AutoAim$2.CAST && mc.thePlayer.ticksExisted - this.castStartTick > var8) {
                    this.beginRodCooldown();
                }
            } else if (this.LvifHr) {
                this.rodState = AutoAim$2.CAST;
                boolean var2 = this.entityPlayer != null && (var1.caughtEntity == this.entityPlayer || this.lnfE04(var1, this.entityPlayer));
                boolean var3 = var1.caughtEntity != null && var1.caughtEntity != this.entityPlayer;
                int var4 = this.aimSolution == null ? 20 : this.aimSolution.getTicks();
                boolean var5 = var1.ticksExisted > var4 + 10;
                boolean var6 = var1.ticksExisted > 3 && !this.TEfdK(var1);
                boolean var7 = this.shouldRecycleHook(var1, var4);
                if (var2) {
                    this.pendingAction = AutoAim$1.REEL_ROD_HIT;
                } else if (var7) {
                    this.pendingAction = AutoAim$1.RECYCLE_ROD;
                } else if (var3 || var5 || var6 || var1.isDead) {
                    this.pendingAction = AutoAim$1.REEL_ROD_MISS;
                }
            }
        }
    }

    private boolean shouldRecycleHook(EntityFishHook var1, int var2) {
        if (this.entityPlayer == null) {
            return true;
        } else {
            double var3 = Math.max(1.0, (double)(var2 - var1.ticksExisted));
            TargetPrediction var5 = this.motionPredictor.ygp3(this.entityPlayer.getEntityId(), this.smoothedLatencyMillis / 100.0 + var3, this.createCollisionCheck(this.entityPlayer));
            return var5 == null || var5.getConfidence() < this.minimumConfidence.number() / 100.0;
        }
    }

    private boolean lnfE04(EntityFishHook var1, EntityPlayer var2) {
        AxisAlignedBB var3 = var2.getEntityBoundingBox().expand(0.35, 0.35, 0.35);
        AxisAlignedBB var4 = var1.getEntityBoundingBox();
        return var4 != null && var3.intersectsWith(var4);
    }

    private boolean TEfdK(EntityFishHook var1) {
        if (this.entityPlayer != null && this.isValidTarget(this.entityPlayer, 360.0)) {
            double var2 = var1.posX;
            double var4 = var1.posY;
            double var6 = var1.posZ;
            double var8 = var1.motionX;
            double var10 = var1.motionY;
            double var12 = var1.motionZ;
            double var14 = this.smoothedLatencyMillis / 100.0;
            MotionPredictor$1 var16 = this.createCollisionCheck(this.entityPlayer);

            for (int var17 = 1; var17 <= 24; var17++) {
                Vec3 var18 = new Vec3(var2, var4, var6);
                Vec3 var19 = new Vec3(var2 + var8, var4 + var10, var6 + var12);
                MovingObjectPosition var20 = mc.theWorld.rayTraceBlocks(var18, var19, false, true, false);
                TargetPrediction var21 = this.motionPredictor.ygp3(this.entityPlayer.getEntityId(), var14 + var17, var16);
                if (var21 == null) {
                    return true;
                }

                double var22 = this.entityPlayer.width * 0.5;
                AxisAlignedBB var24 = new AxisAlignedBB(
                        var21.RAYFZ() - var22, var21.fmmQ(), var21.getZ() - var22, var21.RAYFZ() + var22, var21.fmmQ() + this.entityPlayer.height, var21.getZ() + var22
                    )
                    .expand(0.3, 0.3, 0.3);
                MovingObjectPosition var25 = var24.calculateIntercept(var18, var19);
                if (var25 != null && (var20 == null || var18.squareDistanceTo(var25.hitVec) <= var18.squareDistanceTo(var20.hitVec))) {
                    return true;
                }

                if (var20 != null) {
                    return false;
                }

                var2 += var8;
                var4 += var10;
                var6 += var12;
                var8 *= 0.92;
                var10 = var10 * 0.92 - 0.04;
                var12 *= 0.92;
            }

            return false;
        } else {
            return false;
        }
    }

    private void beginRodCooldown() {
        this.rodState = AutoAim$2.COOLDOWN;
        this.cooldownTicks = this.swappedToRod ? Math.max(1, (int)Math.round(this.cooldown.number() * 20.0)) : 1;
        this.aimSolution = null;
        this.hitStreak = 0;
        this.LvifHr = false;
    }

    private void updateAimOutcome(AimResult var1) {
        if (this.isConfirmedHit(var1)) {
            this.hitStreak++;
            this.missStreak = 0;
        } else if (this.isViableSolution(var1)) {
            this.hitStreak = 0;
            this.missStreak = 0;
        } else {
            this.hitStreak = 0;
            this.missStreak++;
            if (this.missStreak > 20) {
                this.entityPlayer = null;
            }
        }
    }

    private boolean isViableSolution(AimResult var1) {
        return var1 != null && (var1.WYqjD() == AimResult$0.HIT || var1.WYqjD() == AimResult$0.LOW_CONFIDENCE);
    }

    private boolean isConfirmedHit(AimResult var1) {
        return var1 != null && var1.WYqjD() == AimResult$0.HIT;
    }

    private boolean ZRqx(AutoAim$1 var1) {
        return var1 == AutoAim$1.CAST_ROD || var1 == AutoAim$1.RECYCLE_ROD || var1 == AutoAim$1.REEL_ROD_HIT || var1 == AutoAim$1.REEL_ROD_MISS;
    }

    private boolean isWithinAngleTolerance(AimResult var1, double var2, double var4) {
        if (var1 != null && RotationUtils.lastSentRotation != null && RotationUtils.lastSentRotation.length >= 2) {
            double var6 = Math.abs(MathHelper.wrapAngleTo180_float(var1.getYaw() - RotationUtils.lastSentRotation[0]));
            double var8 = Math.abs(var1.getPitch() - RotationUtils.lastSentRotation[1]);
            return var6 <= var2 && var8 <= var4;
        } else {
            return false;
        }
    }

    private boolean isTargetReachable(AimResult var1) {
        TargetPrediction var2 = var1 == null ? null : var1.getPrediction();
        if (var2 != null && !(var2.getConfidence() < 0.18)) {
            double var3 = this.entityPlayer == null ? var2.fmmQ() : this.entityPlayer.serverPosY / 32.0;
            double var5 = var2.fmmQ() - var3;
            PotionEffect var7 = this.entityPlayer == null ? null : this.entityPlayer.getActivePotionEffect(Potion.jump);
            double var8 = 1.5 + (var7 == null ? 0.0 : (var7.getAmplifier() + 1) * 0.75);
            return var5 >= -1.5 && var5 <= var8;
        } else {
            return false;
        }
    }

    private void updatePlayerTracking() {
        this.BXFwSu.clear();

        for (EntityPlayer var2 : mc.theWorld.playerEntities) {
            if (var2 != mc.thePlayer) {
                int var3 = var2.getEntityId();
                EntityPlayer var4 = this.trackedPlayersById.put(var3, var2);
                if (!var2.isDead && var2.deathTime <= 0 && !(var2.getHealth() <= 0.0F)) {
                    this.BXFwSu.add(var3);
                    double var5 = var2.serverPosX / 32.0;
                    double var7 = var2.serverPosY / 32.0;
                    double var9 = var2.serverPosZ / 32.0;
                    boolean var11 = this.TcE.remove(var3) || var4 != null && var4 != var2;
                    if (var11) {
                        this.motionPredictor.AVgoK(var3);
                        this.entityPositionTracker.removeEntity(var3);
                        this.entityPositionTracker.MVkfPjx(var3, var5, var7, var9);
                        if (this.entityPlayer != null && this.entityPlayer.getEntityId() == var3) {
                            this.clearAimSolution(true);
                            this.resetAimVisualisation();
                        }
                    }

                    ItemStack var13 = var2.getItemInUse();
                    ItemStack var14 = var2.getHeldItem();
                    boolean var15 = var2.isBlocking() || var2.isUsingItem() && var13 != null && var13.getItem() instanceof ItemBow;
                    boolean var16 = var14 != null && var14.getItem() instanceof ItemBlock && var2.rotationPitch > 70.0F;
                    this.motionPredictor.updateMotionStateInternal(var3, var15, var2.isSprinting(), var2.onGround, var16);
                    if (this.motionPredictor.hasSamples(var3) && this.motionPredictor.computeSquaredDistanceToLastSample(var3, var5, var7, var9) > 2.25) {
                        this.motionPredictor.AVgoK(var3);
                        this.entityPositionTracker.MVkfPjx(var3, var5, var7, var9);
                    } else if (!this.motionPredictor.hasSamples(var3)) {
                        this.entityPositionTracker.trackInitialPosition(var3, var5, var7, var9);
                    }
                } else {
                    this.TcE.add(var3);
                    this.motionPredictor.AVgoK(var3);
                    this.entityPositionTracker.removeEntity(var3);
                    if (this.entityPlayer != null && this.entityPlayer.getEntityId() == var3) {
                        this.clearAimSolution(true);
                        this.resetAimVisualisation();
                    }
                }
            }
        }

        if (this.entityPlayer != null && !this.BXFwSu.contains(this.entityPlayer.getEntityId())) {
            this.motionPredictor.AVgoK(this.entityPlayer.getEntityId());
            this.entityPlayer = null;
        }

        NetworkPlayerInfo var17 = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        if (var17 != null && var17.getResponseTime() >= 0) {
            double var18 = Math.min(1000.0, (double)var17.getResponseTime());
            this.smoothedLatencyMillis = this.smoothedLatencyMillis == 0.0 ? var18 : this.smoothedLatencyMillis * 0.85 + var18 * 0.15;
        }
    }

    private void purgeRemovedPlayers() {
        int[] var1 = this.entityPositionTracker.Thdhe();

        for (int var5 : var1) {
            this.motionPredictor.AVgoK(var5);
            this.TcE.add(var5);
            this.trackedPlayersById.remove(var5);
            if (this.entityPlayer != null && this.entityPlayer.getEntityId() == var5) {
                this.clearAimSolution(true);
                this.resetAimVisualisation();
            }
        }
    }

    private EntityPlayer DEhn(EntityPlayer var1) {
        double var2 = this.fov.number();
        boolean var4 = var1 != null && this.isValidTarget(var1, var2);
        double var5 = var4 ? this.getCrosshairAngle(var1) : Double.MAX_VALUE;
        EntityPlayer var7 = null;
        double var8 = var2;

        for (EntityPlayer var11 : mc.theWorld.playerEntities) {
            if (this.isValidTarget(var11, var2)) {
                double var12 = this.getCrosshairAngle(var11);
                if (var12 < var8) {
                    var8 = var12;
                    var7 = var11;
                }
            }
        }

        return var4 && var7 != var1 && !YJPV(var5, var8) ? var1 : var7;
    }

    private EntityPlayer selectRodTarget(EntityPlayer var1) {
        double var2 = this.fov.number();
        boolean var4 = var1 != null && this.isValidRodTarget(var1, var2);
        double var5 = var4 ? this.getCrosshairAngle(var1) : Double.MAX_VALUE;
        EntityPlayer var7 = null;
        double var8 = var2;

        for (EntityPlayer var11 : mc.theWorld.playerEntities) {
            if (this.isValidRodTarget(var11, var2)) {
                double var12 = this.getCrosshairAngle(var11);
                if (var12 < var8) {
                    var8 = var12;
                    var7 = var11;
                }
            }
        }

        return var4 && var7 != var1 && !YJPV(var5, var8) ? var1 : var7;
    }

    public static boolean YJPV(double var0, double var2) {
        return var2 + 1.25 < var0;
    }

    private boolean isValidRodTarget(EntityPlayer var1, double var2) {
        if (!this.isValidTarget(var1, var2)) {
            return false;
        } else if (this.opponentLooking.bool() && !this.WCrjIv(var1)) {
            return false;
        } else {
            double var4 = Math.min(this.minimumRodRange.number(), this.maximumRodRange.number());
            double var6 = Math.max(this.minimumRodRange.number(), this.maximumRodRange.number());
            double var8 = mc.thePlayer.getDistanceSqToEntity(var1);
            return var8 >= var4 * var4 && var8 <= var6 * var6;
        }
    }

    private boolean WCrjIv(EntityPlayer var1) {
        double var2 = mc.thePlayer.posX - var1.posX;
        double var4 = mc.thePlayer.posZ - var1.posZ;
        double var6 = Math.sqrt(var2 * var2 + var4 * var4);
        if (var6 < 1.0E-6) {
            return true;
        } else {
            double var8 = Math.toRadians(var1.rotationYawHead);
            double var10 = -Math.sin(var8);
            double var12 = Math.cos(var8);
            double var14 = var10 * (var2 / var6) + var12 * (var4 / var6);
            return var14 >= Math.cos(Math.toRadians(80.0));
        }
    }

    private boolean isValidTarget(EntityPlayer var1, double var2) {
        if (var1 == null || var1 == mc.thePlayer || var1.isDead || var1.deathTime != 0 || var1.getHealth() <= 0.0F) {
            return false;
        } else if (mc.theWorld == null || mc.theWorld.getEntityByID(var1.getEntityId()) != var1 || !mc.theWorld.playerEntities.contains(var1)) {
            return false;
        } else if (!ClientUtils.isFriend(var1) && !AntiBot.shouldHideEntity(var1)) {
            boolean var4 = ClientUtils.isTeammate(var1);
            if (var4 ? this.teammates.bool() : this.players.bool()) {
                if (!this.invisible.bool() && var1.isInvisible()) {
                    return false;
                } else if (mc.thePlayer.getDistanceSqToEntity(var1) > this.range.number() * this.range.number()) {
                    return false;
                } else {
                    return !mc.thePlayer.canEntityBeSeen(var1) ? false : this.getCrosshairAngle(var1) <= var2;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private double getCrosshairAngle(EntityPlayer var1) {
        double var2 = var1.posX - mc.thePlayer.posX;
        double var4 = var1.posZ - mc.thePlayer.posZ;
        double var6 = var1.posY + var1.getEyeHeight() * 0.8 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double var8 = Math.sqrt(var2 * var2 + var4 * var4);
        float var10 = (float)Math.toDegrees(Math.atan2(var4, var2)) - 90.0F;
        float var11 = (float)(-Math.toDegrees(Math.atan2(var6, var8)));
        double var12 = MathHelper.wrapAngleTo180_float(var10 - mc.thePlayer.rotationYaw);
        double var14 = var11 - mc.thePlayer.rotationPitch;
        return Math.sqrt(var12 * var12 + var14 * var14);
    }

    private boolean canOperate() {
        return ClientUtils.isInWorld() && mc.currentScreen == null && mc.inGameHasFocus && !mc.thePlayer.isDead;
    }

    private boolean isBowFullyDrawn() {
        ItemStack var1 = mc.thePlayer == null ? null : mc.thePlayer.getHeldItem();
        ItemStack var2 = mc.thePlayer == null ? null : mc.thePlayer.getItemInUse();
        return var1 != null && var1.getItem() instanceof ItemBow && var2 != null && var2.getItem() instanceof ItemBow && mc.thePlayer.getItemInUseCount() > 0;
    }

    private boolean isUsingBow() {
        if (this.bow.bool() && mc.thePlayer != null && Mouse.isButtonDown(1)) {
            ItemStack var1 = mc.thePlayer.getHeldItem();
            return var1 != null && var1.getItem() instanceof ItemBow;
        } else {
            return false;
        }
    }

    private boolean isHoldingRod() {
        ItemStack var1 = mc.thePlayer == null ? null : mc.thePlayer.getHeldItem();
        return var1 != null && var1.getItem() == Items.fishing_rod;
    }

    private boolean PMaFht() {
        return (int)Math.round(this.rodMode.choice()) == 1;
    }

    private boolean areAutomaticConditionsMet() {
        if (this.notWhilstHoldingLeft.bool() && Mouse.isButtonDown(0)) {
            return false;
        } else {
            return this.notWhilstHoldingRight.bool() && Mouse.isButtonDown(1) ? false : !this.holdingWeapon.bool() || ClientUtils.isHoldingWeapon();
        }
    }

    private int getBowFlightTicks() {
        return Math.max(1, (int)Math.round(this.bowFlight.number() * 20.0));
    }

    private int findHotbarSlot(Item var1) {
        if (mc.thePlayer == null) {
            return -1;
        } else {
            for (int var2 = 0; var2 < 9; var2++) {
                ItemStack var3 = mc.thePlayer.inventory.getStackInSlot(var2);
                if (var3 != null && var3.getItem() == var1) {
                    return var2;
                }
            }

            return -1;
        }
    }

    private int findSwordHotbarSlot() {
        if (mc.thePlayer == null) {
            return -1;
        } else {
            int var1 = mc.thePlayer.inventory.currentItem;
            ItemStack var2 = mc.thePlayer.inventory.getStackInSlot(var1);
            if (var2 != null && var2.getItem() instanceof ItemSword) {
                return var1;
            } else {
                for (int var3 = 0; var3 < 9; var3++) {
                    ItemStack var4 = mc.thePlayer.inventory.getStackInSlot(var3);
                    if (var4 != null && var4.getItem() instanceof ItemSword) {
                        return var3;
                    }
                }

                return -1;
            }
        }
    }

    private void selectHotbarSlot(int var1) {
        if (ClientUtils.isInWorld() && var1 >= 0 && var1 <= 8 && var1 != mc.thePlayer.inventory.currentItem) {
            mc.thePlayer.inventory.currentItem = var1;
            mc.playerController.syncCurrentPlayItem();
        }
    }

    private void restoreHotbarSlot() {
        if (this.swappedToRod) {
            if (ClientUtils.isInWorld() && this.isHoldingRod()) {
                this.selectHotbarSlot(this.Vx3);
            }

            this.swappedToRod = false;
            this.Vx3 = -1;
        }
    }

    private double getBowDrawProgress() {
        return ProjectileMotion.OYIzkT(72000 - mc.thePlayer.getItemInUseCount());
    }

    private double getLateralLeadCorrection(EntityPlayer var1) {
        if (var1 != null && mc.thePlayer != null) {
            double[] var2 = this.motionPredictor.TjaA(var1.getEntityId());
            double var3 = var1.serverPosX / 32.0 - mc.thePlayer.posX;
            double var5 = var1.serverPosZ / 32.0 - mc.thePlayer.posZ;
            return 4.0 * squaredSineBetween(var3, var5, var2[0], var2[1]);
        } else {
            return 0.0;
        }
    }

    public static double squaredSineBetween(double var0, double var2, double var4, double var6) {
        double var8 = Math.sqrt(var0 * var0 + var2 * var2);
        double var10 = Math.sqrt(var4 * var4 + var6 * var6);
        if (!(var8 < 1.0E-6) && !(var10 < 0.04)) {
            double var12 = Math.abs(var0 * var6 - var2 * var4);
            double var14 = var12 / (var8 * var10);
            return Math.max(0.0, Math.min(1.0, var14 * var14));
        } else {
            return 0.0;
        }
    }

    private void smoothTowardTargetAngles(RotationEvent var1, float var2, float var3) {
        if (!this.srOx1) {
            this.smoothedYaw = var1.yaw != null ? var1.yaw : RotationUtils.lastSentRotation[0];
            this.LjSl = var1.pitch != null ? var1.pitch : RotationUtils.lastSentRotation[1];
            this.srOx1 = true;
        }

        this.smoothedYaw = stepYawToward(this.smoothedYaw, var2);
        this.LjSl = stepPitchToward(this.LjSl, var3);
        this.applySmoothedRotation(var1);
    }

    private void applySmoothedRotation(RotationEvent var1) {
        if (this.srOx1) {
            RotationHandler.getInstance().ljYma8(false);
            var1.setRotation(this.smoothedYaw, this.LjSl, 55);
        }
    }

    private void driftTowardPlayerAngles(RotationEvent var1) {
        if (this.srOx1 && mc.thePlayer != null) {
            this.smoothedYaw = stepYawToward(this.smoothedYaw, mc.thePlayer.rotationYaw);
            this.LjSl = stepPitchToward(this.LjSl, mc.thePlayer.rotationPitch);
            this.applySmoothedRotation(var1);
        }
    }

    public static float stepYawToward(float var0, float var1) {
        float var2 = MathHelper.wrapAngleTo180_float(var1 - var0);
        return var0 + gcPk(var2, 40.0F);
    }

    public static float stepPitchToward(float var0, float var1) {
        float var2 = var1 - var0;
        return MathHelper.clamp_float(var0 + gcPk(var2, 15.0F), -90.0F, 90.0F);
    }

    private static float gcPk(float var0, float var1) {
        return Math.abs(var0) <= 0.08F ? var0 : MathHelper.clamp_float(var0 * 0.45F, -var1, var1);
    }

    private void clearRotationSmoothing() {
        this.srOx1 = false;
    }

    private void clearAimSolution(boolean var1) {
        this.aimSolution = null;
        this.hitStreak = 0;
        this.pendingAction = AutoAim$1.NONE;
        if (var1) {
            this.entityPlayer = null;
        }
    }

    private void resetCombatState() {
        this.restoreHotbarSlot();
        this.entityPlayer = null;
        this.aimSolution = null;
        this.pendingAction = AutoAim$1.NONE;
        this.rodState = AutoAim$2.READY;
        this.hitStreak = 0;
        this.cooldownTicks = 0;
        this.missStreak = 0;
        this.LvifHr = false;
        this.swappedToRod = false;
        this.Vx3 = -1;
        this.clearRotationSmoothing();
        this.resetAimVisualisation();
    }

    private void resetAimVisualisation() {
        this.visualisedEntityId = -1;
        this.lastVisualisationNanos = 0L;
        this.aimIndicatorInitialised = false;
    }

    private void resetModuleState() {
        this.resetCombatState();
        this.motionPredictor.resetAll();
        this.entityPositionTracker.ELFwOy();
        this.BXFwSu.clear();
        this.TcE.clear();
        this.trackedPlayersById.clear();
        this.smoothedLatencyMillis = 0.0;
        RotationHandler.getInstance().NkF5();
    }

    @Subscribe(priority = Subscribe.Priority.HIGHEST)
    public void onRenderWorldLast(RenderWorldLastEvent var1) {
        if (this.visualise.bool() && this.entityPlayer != null && ClientUtils.isInWorld()) {
            TargetPrediction var2 = this.aimSolution == null ? null : this.aimSolution.getPrediction();
            if (var2 == null) {
                var2 = this.motionPredictor.ygp3(this.entityPlayer.getEntityId(), this.smoothedLatencyMillis / 100.0 + 10.0, this.createCollisionCheck(this.entityPlayer));
            }

            if (var2 != null) {
                double[] var3 = this.uMiavU(var2);
                long var4 = System.nanoTime();
                double var6 = this.lastVisualisationNanos == 0L ? 1.0 : Math.min(0.1, Math.max(0.0, (var4 - this.lastVisualisationNanos) / 1.0E9));
                double var8 = 1.0 - Math.exp(-var6 * 14.0);
                double var10 = this.square(this.smoothedEntityX - var3[0]) + this.square(this.smoothedEntityY - var3[1]) + this.square(this.smoothedEntityZ - var3[2]);
                if (this.visualisedEntityId == this.entityPlayer.getEntityId() && this.lastVisualisationNanos != 0L && !(var10 > 36.0)) {
                    this.smoothedEntityX = this.smoothedEntityX + (var3[0] - this.smoothedEntityX) * var8;
                    this.smoothedEntityY = this.smoothedEntityY + (var3[1] - this.smoothedEntityY) * var8;
                    this.smoothedEntityZ = this.smoothedEntityZ + (var3[2] - this.smoothedEntityZ) * var8;
                } else {
                    this.visualisedEntityId = this.entityPlayer.getEntityId();
                    this.smoothedEntityX = var3[0];
                    this.smoothedEntityY = var3[1];
                    this.smoothedEntityZ = var3[2];
                    this.aimIndicatorInitialised = false;
                }

                this.lastVisualisationNanos = var4;
                int var12 = Theme.accent().argb();
                float var13 = (var12 >> 16 & 0xFF) / 255.0F;
                float var14 = (var12 >> 8 & 0xFF) / 255.0F;
                float var15 = (var12 & 0xFF) / 255.0F;
                double var16 = mc.getRenderManager().viewerPosX;
                double var18 = mc.getRenderManager().viewerPosY;
                double var20 = mc.getRenderManager().viewerPosZ;
                double var22 = this.entityPlayer.width * 0.5;
                AxisAlignedBB var24 = new AxisAlignedBB(
                    this.smoothedEntityX - var22, this.smoothedEntityY, this.smoothedEntityZ - var22, this.smoothedEntityX + var22, this.smoothedEntityY + this.entityPlayer.height, this.smoothedEntityZ + var22
                );
                GL11.glPushAttrib(1048575);
                GL11.glPushMatrix();

                try {
                    GL11.glDisable(3553);
                    GL11.glDisable(2929);
                    GL11.glEnable(3042);
                    GL11.glBlendFunc(770, 771);
                    GL11.glDepthMask(false);
                    GL11.glLineWidth(2.0F);
                    RenderUtils.drawFilledAabb(var24.offset(-var16, -var18, -var20), var13, var14, var15, 0.25F);
                    GL11.glColor4f(var13, var14, var15, 1.0F);
                    RenderUtils.drawOffsetBoxOutline(var24, var16, var18, var20);
                    Vec3 var25 = this.aimSolution == null ? null : this.aimSolution.kahN();
                    if (var25 != null) {
                        double var26 = Math.max(this.smoothedEntityX - var22, Math.min(this.smoothedEntityX + var22, var25.xCoord));
                        double var28 = Math.max(this.smoothedEntityY, Math.min(this.smoothedEntityY + this.entityPlayer.height, var25.yCoord));
                        double var30 = Math.max(this.smoothedEntityZ - var22, Math.min(this.smoothedEntityZ + var22, var25.zCoord));
                        if (!this.aimIndicatorInitialised) {
                            this.FfVm = var26;
                            this.smoothedAimY = var28;
                            this.smoothedAimZ = var30;
                            this.aimIndicatorInitialised = true;
                        } else {
                            this.FfVm = this.FfVm + (var26 - this.FfVm) * var8;
                            this.smoothedAimY = this.smoothedAimY + (var28 - this.smoothedAimY) * var8;
                            this.smoothedAimZ = this.smoothedAimZ + (var30 - this.smoothedAimZ) * var8;
                        }

                        double var32 = 0.14;
                        AxisAlignedBB var34 = new AxisAlignedBB(
                            this.FfVm - var32, this.smoothedAimY - var32, this.smoothedAimZ - var32, this.FfVm + var32, this.smoothedAimY + var32, this.smoothedAimZ + var32
                        );
                        RenderUtils.drawFilledAabb(var34.offset(-var16, -var18, -var20), var13, var14, var15, 0.25F);
                        GL11.glColor4f(var13, var14, var15, 1.0F);
                        RenderUtils.drawOffsetBoxOutline(var34, var16, var18, var20);
                    }
                } finally {
                    GL11.glPopMatrix();
                    GL11.glPopAttrib();
                    RenderUtils.resetGlPipelineState();
                }
            }
        }
    }

    private double[] uMiavU(TargetPrediction var1) {
        double var2 = this.entityPlayer.serverPosX / 32.0;
        double var4 = this.entityPlayer.serverPosY / 32.0;
        double var6 = this.entityPlayer.serverPosZ / 32.0;
        double var8 = this.smoothedLatencyMillis / 100.0 + (this.aimSolution == null ? 10.0 : this.aimSolution.getTicks());
        double var10 = Math.max(2.0, Math.min(8.0, 1.25 + var8 * 0.38));
        double var12 = var1.RAYFZ() - var2;
        double var14 = var1.getZ() - var6;
        double var16 = Math.sqrt(var12 * var12 + var14 * var14);
        if (var16 > var10 && var16 > 1.0E-6) {
            var12 *= var10 / var16;
            var14 *= var10 / var16;
        }

        PotionEffect var18 = this.entityPlayer.getActivePotionEffect(Potion.jump);
        double var19 = 1.5 + (var18 == null ? 0.0 : (var18.getAmplifier() + 1) * 0.75);
        double var21 = Math.max(var4 - 1.5, Math.min(var4 + var19, var1.fmmQ()));
        return new double[]{var2 + var12, var21, var6 + var14};
    }

    private MotionPredictor$1 createCollisionCheck(final EntityPlayer var1) {
        return new MotionPredictor$1() {
            @Override
            public boolean isPositionClear(double var1x, double var3, double var5) {
                double var7 = var1.width * 0.5;
                AxisAlignedBB var9 = new AxisAlignedBB(var1x - var7, var3, var5 - var7, var1x + var7, var3 + var1.height, var5 + var7);
                return AutoAim.getMinecraftInstance().theWorld != null && AutoAim.getMc().theWorld.getCollidingBoundingBoxes(var1, var9).isEmpty();
            }
        };
    }

    private double square(double var1) {
        return var1 * var1;
    }

    public static Minecraft getMinecraftInstance() {
        return mc;
    }

    public static Minecraft getMc() {
        return mc;
    }
}
