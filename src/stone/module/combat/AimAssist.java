package stone.module.combat;

import stone.event.RotationEvent;
import stone.event.Subscribe;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.ModuleManager;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.combat.aimassist.AimAssistMode;
import stone.module.combat.shared.BreakableBlockWhitelist;
import stone.module.other.AntiBot;
import stone.module.player.BedNuker;
import stone.module.player.BridgeNuker;
import stone.util.ClientUtils;
import stone.util.block.BlockList;
import stone.util.rotation.RotationHandler;
import stone.util.rotation.RotationUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import org.lwjgl.input.Mouse;

public class AimAssist extends Module {
    private Setting mode;
    private Setting speed;
    private Setting multipoint;
    private Setting randomization;
    private Setting fov;
    private Setting distance;
    private Setting sort;
    private Setting aimInvis;
    private Setting requireMouse;
    private Setting teammates;
    private Setting ignoreBehindWalls;
    private Setting targetBehindFriendly;
    private Setting targetBehindEnemy;
    private Setting stopWhenBreaking;
    private Setting breakingBlocksWhitelist;
    private BlockList whitelistedBreakingBlocks;
    private Setting weaponOnly;
    private long kaY = -1L;
    private static final String[] uGubL = new String[]{"Health", "FOV", "Hurttime", "Distance"};

    public AimAssist() {
        super("AimAssist", "Pulls your aim toward the nearest target", Category.COMBAT, Subcategory.BASIC);
        this.mode = addChoice("Mode", "", List.of(AimAssistMode.labels()), AimAssistMode.NORMAL.ordinal());
        this.speed = addNumber("Speed", "", 10.0F, 1.0F, 30.0F, 0);
        this.multipoint = addNumber("Multipoint", "", 0.0F, 0.0F, 100.0F, 0);
        this.randomization = addNumber("Randomization", "", 50.0F, 0.0F, 100.0F, 0);
        this.fov = addNumber("FOV", "", 90.0F, 15.0F, 360.0F, 0);
        this.distance = addNumber("Distance", "", 4.5F, 0.0F, 5.0F, 1);
        this.sort = addChoice("Sort", "", List.of(uGubL), 1);
        this.ignoreBehindWalls = addBoolean("Ignore behind walls", "", false);
        this.targetBehindFriendly = addBoolean("Target behind Friendly", "", true);
        this.targetBehindEnemy = addBoolean("Target behind Enemy", "", true);
        this.aimInvis = addBoolean("Aim invis", "", false);
        this.requireMouse = addBoolean("Require mouse", "", true);
        this.teammates = addBoolean("Teammates", "", false);
        this.stopWhenBreaking = addBoolean("Stop when breaking", "", false);
        this.breakingBlocksWhitelist = addBoolean("Breaking Blocks Whitelist", "", false);
        this.whitelistedBreakingBlocks = new BlockList();
        BreakableBlockWhitelist.TdpPo7(this.whitelistedBreakingBlocks);
        this.breakingBlocksWhitelist.setVisible(false);
        this.weaponOnly = addBoolean("Weapon only", "", false);
    }

    @Override
    public String suffix() {
        return this.KVfE96().getLabel();
    }

    public EntityPlayer GHPA() {
        if (this.enabled() && ClientUtils.isInWorld() && this.canAimWithMouseHeld()) {
            Entity var1 = this.findTarget(this.isSilentMode());
            return var1 instanceof EntityPlayer ? (EntityPlayer)var1 : null;
        } else {
            return null;
        }
    }

    public boolean hasTarget() {
        return this.enabled() && ClientUtils.isInWorld() && this.canAim(false) && this.findTarget(this.isSilentMode()) != null;
    }

    public EntityPlayer getTargetPlayer() {
        if (this.enabled() && ClientUtils.isInWorld() && this.canAim(false)) {
            Entity var1 = this.findTarget(this.isSilentMode());
            return var1 instanceof EntityPlayer ? (EntityPlayer)var1 : null;
        } else {
            return null;
        }
    }

    @Override
    public void guiUpdate() {
        boolean var1 = this.isLockOnMode();
        this.speed.setVisible(!var1);
        this.multipoint.setVisible(!var1);
        this.randomization.setVisible(!var1);
        this.breakingBlocksWhitelist.setVisible(this.stopWhenBreaking.bool());
    }

    @Override
    public void onDisable(Context context) {
        this.kaY = -1L;
    }

    @Subscribe(priority = Subscribe.Priority.NORMAL)
    public void onRotation(RotationEvent var1) {
        if (ModuleManager.get(BedNuker.class) == null || !ModuleManager.get(BedNuker.class).shouldOverridePointedObject()) {
            if (ModuleManager.get(BridgeNuker.class) == null || !ModuleManager.get(BridgeNuker.class).shouldOverridePointedObject()) {
                if (this.isSilentMode() && this.canAimWithMouseHeld()) {
                    Entity var2 = this.findTarget(true);
                    if (var2 != null) {
                        float[] var3 = this.computeSilentRotation(var2, var1);
                        if (var3 != null) {
                            RotationHandler.getInstance().ljYma8(false);
                            var1.setRotation(var3[0], var3[1], 10);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onUpdate() {
        if (!this.isSilentMode() && this.canAimWithMouseHeld()) {
            Entity var1 = this.findTarget(false);
            if (var1 != null) {
                float[] var2 = this.PCzIr(var1);
                if (var2 != null) {
                    var2 = RotationUtils.NSsr(var2[0], var2[1], mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                    mc.thePlayer.rotationYaw = var2[0];
                    mc.thePlayer.rotationPitch = var2[1];
                }
            }
        }
    }

    private boolean isSilentMode() {
        return this.KVfE96() == AimAssistMode.SILENT;
    }

    private boolean isLockOnMode() {
        return this.KVfE96() == AimAssistMode.LOCK_ON;
    }

    private AimAssistMode KVfE96() {
        return AimAssistMode.fromSetting(this.mode.choice());
    }

    private float[] computeSilentRotation(Entity var1, RotationEvent var2) {
        double var3 = this.getEffectiveMultipoint();
        boolean var5 = this.ebdGdj();
        return RotationHandler.getInstance()
            .getEventEntityRotationsFull(var1, var2, this.getEffectiveSpeed(), var3, var3, this.getEffectiveRandomization(), var5, this.distance.number(), !this.ignoreBehindWalls.bool(), this.targetBehindFriendly.bool(), this.targetBehindEnemy.bool());
    }

    private float[] PCzIr(Entity var1) {
        double var2 = this.getEffectiveMultipoint();
        boolean var4 = this.ebdGdj();
        return RotationHandler.getInstance()
            .getEntityRotationsFull(var1, this.getEffectiveSpeed(), var2, var2, this.getEffectiveRandomization(), var4, this.distance.number(), !this.ignoreBehindWalls.bool(), this.targetBehindFriendly.bool(), this.targetBehindEnemy.bool());
    }

    private int getEffectiveSpeed() {
        return this.isLockOnMode() ? 30 : (int)this.speed.number();
    }

    private double getEffectiveMultipoint() {
        return this.isLockOnMode() ? 0.0 : this.multipoint.number();
    }

    private float getEffectiveRandomization() {
        return this.isLockOnMode() ? 0.0F : (float)this.randomization.number();
    }

    private Entity findTarget(boolean var1) {
        int var2 = (int)this.fov.number();
        double var3 = this.distance.number();
        double var5 = var3 * var3;
        boolean var7 = this.ebdGdj();
        boolean var8 = this.teammates.bool();
        boolean var9 = this.aimInvis.bool();
        int var10 = (int)this.sort.choice();
        float var11 = mc.thePlayer.rotationYaw;
        if (var1) {
            Float var12 = RotationHandler.getInstance().getTargetYaw();
            if (var12 != null) {
                var11 = var12;
            }
        }

        AutoFeed var22 = ModuleManager.get(AutoFeed.class);
        ArrayList<AimAssist$1> var13 = var7 ? new ArrayList<AimAssist$1>() : null;
        AimAssist$1 var14 = null;

        for (EntityPlayer var16 : mc.theWorld.playerEntities) {
            if (var16 != mc.thePlayer
                && var16.deathTime == 0
                && !ClientUtils.isFriend(var16)
                && (var8 || !ClientUtils.isTeammate(var16))
                && (var9 || !var16.isInvisible())
                && (var22 == null || !var22.wouldCancelAttack(var16))) {
                double var17 = RotationUtils.getDistanceSqToEntity(var16);
                if (!(var17 > var5) && !AntiBot.shouldHideEntity(var16)) {
                    if (var2 != 360) {
                        float var19 = RotationUtils.ilaZ(var16.posX, var16.posZ);
                        if (!ClientUtils.Bhr95(var11, var2, var19)) {
                            continue;
                        }
                    }

                    double var25 = mc.thePlayer.getDistanceSqToEntity(var16);
                    AimAssist$1 var21 = new AimAssist$1(var16, computeSortMetric(var16, var10, var25), var25);
                    if (var7) {
                        var13.add(var21);
                    } else if (var14 == null || compareTargets(var21, var14) < 0) {
                        var14 = var21;
                    }
                }
            }
        }

        if (!var7) {
            return var14 == null ? null : AimAssist$1.getPlayer(var14);
        } else if (var13.isEmpty()) {
            return null;
        } else {
            var13.sort(AimAssist::compareTargets);
            double var23 = this.isLockOnMode() ? 0.0 : this.multipoint.number();
            boolean var24 = !this.ignoreBehindWalls.bool();
            boolean var18 = this.targetBehindFriendly.bool();
            boolean var26 = this.targetBehindEnemy.bool();

            for (AimAssist$1 var27 : (java.lang.Iterable<AimAssist$1>) (java.lang.Iterable<?>) (var13)) {
                if (RotationUtils.XKXgx5(AimAssist$1.getPlayer(var27), var23, var23, var3, var24, var18, var26)) {
                    return AimAssist$1.getPlayer(var27);
                }
            }

            return null;
        }
    }

    private boolean ebdGdj() {
        return this.ignoreBehindWalls.bool() || !this.targetBehindFriendly.bool() || !this.targetBehindEnemy.bool();
    }

    private static double computeSortMetric(EntityPlayer var0, int var1, double var2) {
        switch (var1) {
            case 0:
                return var0.getHealth() + var0.getAbsorptionAmount();
            case 1:
                return Math.abs(ClientUtils.getYawDifferenceToEntity(var0, false)) + Math.abs(ClientUtils.coPw(var0, false));
            case 2:
                return var0.hurtTime;
            case 3:
                return var2;
            default:
                return Math.abs(ClientUtils.getYawDifferenceToEntity(var0, false)) + Math.abs(ClientUtils.coPw(var0, false));
        }
    }

    private static int compareTargets(AimAssist$1 var0, AimAssist$1 var1) {
        int var2 = Double.compare(AimAssist$1.Dxq6(var0), AimAssist$1.Dxq6(var1));
        return var2 != 0 ? var2 : Double.compare(AimAssist$1.getDistanceSquared(var0), AimAssist$1.getDistanceSquared(var1));
    }

    private boolean canAimWithMouseHeld() {
        return this.canAim(true);
    }

    private boolean canAim(boolean var1) {
        if (mc.currentScreen != null || !mc.inGameHasFocus) {
            return false;
        } else if (this.weaponOnly.bool() && !ClientUtils.isHoldingWeapon()) {
            return false;
        } else if (var1 && this.requireMouse.bool() && !Mouse.isButtonDown(0)) {
            return false;
        } else {
            if (this.stopWhenBreaking.bool() && ClientUtils.isMiningBlock() && this.isTargetedBlockWhitelisted()) {
                if (this.kaY == -1L) {
                    this.kaY = System.currentTimeMillis();
                }

                long var2 = System.currentTimeMillis() - this.kaY;
                if (var2 >= 50L) {
                    return false;
                }
            } else {
                this.kaY = -1L;
            }

            return true;
        }
    }

    private boolean isTargetedBlockWhitelisted() {
        if (!this.breakingBlocksWhitelist.bool()) {
            return true;
        } else {
            MovingObjectPosition var1 = RotationUtils.traceBlockHit(mc.playerController.getBlockReachDistance(), mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
            BlockPos var2 = var1 != null && var1.typeOfHit == MovingObjectType.BLOCK ? var1.getBlockPos() : null;
            return BreakableBlockWhitelist.isBlockWhitelisted(mc, var2, this.whitelistedBreakingBlocks);
        }
    }

}
