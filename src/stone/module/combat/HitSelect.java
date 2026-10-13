package stone.module.combat;

import stone.event.ClickMouseEvent;
import stone.event.PrePlayerInteractEvent;
import stone.event.Subscribe;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Subcategory;
import stone.module.combat.hitselect.HitSelectMode;
import stone.module.combat.hitselect.HitSelectSettings;
import stone.module.combat.hitselect.HitSelectTracker;
import stone.module.combat.hitselect.HitSelectUtils;
import stone.module.combat.hitselect.PlayerHitTracker;
import stone.module.combat.hitselect.RaytraceHitClassifier;
import stone.module.combat.hitselect.RaytraceHitClassifier$0;
import stone.module.shared.TargetFinder;
import stone.util.ClientUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;

public class HitSelect extends Module {
    private final HitSelectSettings hitSelectSettings;
    private final HitSelectTracker combatTracker = new HitSelectTracker();
    private int tickCounter;

    public HitSelect() {
        super("HitSelect", "Holds hits back until they land for more", Category.COMBAT, Subcategory.BASIC);
        this.hitSelectSettings = new HitSelectSettings(this);
    }

    @Override
    public String suffix() {
        return this.getCurrentMode().getLabel();
    }

    @Override
    public void onEnable(Context context) {
        this.tickCounter = 0;
        this.qjoj();
    }

    @Override
    public void onDisable(Context context) {
        this.qjoj();
    }

    @Subscribe
    public void onPrePlayerInteract(PrePlayerInteractEvent var1) {
        if (ClientUtils.isInWorld() && !mc.thePlayer.isDead && mc.theWorld != null) {
            this.tickCounter++;
            int var2 = this.tickCounter;
            this.combatTracker.pruneInvalidPlayers(mc.theWorld);
            EntityPlayer var3 = TargetFinder.findTarget(9.0);
            this.recordAttack(var3, var2);
            this.updateHurtTimeTracking(var2);
            this.trackAttackConfirmation(var2);
        } else {
            this.qjoj();
        }
    }

    @Subscribe
    public void onClickMouse(ClickMouseEvent var1) {
        if (this.kbyrwtb()) {
            int var2 = this.tickCounter;
            RaytraceHitClassifier$0 var3 = RaytraceHitClassifier.classifyRaytraceResult(var1.movingObjectPosition, 9.0);
            if (var3 != RaytraceHitClassifier$0.BLOCK) {
                if (var3 == RaytraceHitClassifier$0.MISS) {
                    if (this.rollChance(this.hitSelectSettings.missedSwings.number())) {
                        this.cancelClick(var1);
                    }
                } else {
                    EntityPlayer var4 = TargetFinder.TDBVqmH(var1.movingObjectPosition == null ? null : var1.movingObjectPosition.entityHit, 9.0);
                    if (var4 != null) {
                        this.recordAttack(var4, var2);
                        PlayerHitTracker var5 = this.VMsy(var4);
                        int var6 = this.computeAttackFlags(var5, var2);
                        boolean var7 = (var6 & 1) != 0 || (var6 & 16) != 0 || this.isPauseTriggered(var5, var6 & -17, var2);
                        if (var7 && this.rollChance(this.hitSelectSettings.inCombat.number())) {
                            this.cancelClick(var1);
                        } else {
                            this.ZUbdxZ0(var4, var2);
                        }
                    }
                }
            }
        }
    }

    private boolean kbyrwtb() {
        return ClientUtils.isInWorld() && mc.theWorld != null && mc.thePlayer != null && !mc.thePlayer.isDead;
    }

    private void cancelClick(ClickMouseEvent var1) {
        if (this.hitSelectSettings.fakeSwing.bool() && ClientUtils.isInWorld()) {
            ClientUtils.startSwing();
        }

        var1.setCancelled(true);
    }

    private void recordAttack(EntityPlayer var1, int var2) {
        this.combatTracker.updateTrackedTarget(var1, var2, this.hitSelectSettings.useServerAttackTime.bool());
    }

    private void updateHurtTimeTracking(int var1) {
        int var2 = mc.thePlayer.hurtTime;
        EntityPlayer var3 = this.combatTracker.getTrackedTarget();
        if (this.combatTracker.getLocalPlayerState().YLCHh(var2, mc.thePlayer.onGround) && var3 != null) {
            this.VMsy(var3).LDYAtZ();
        }
    }

    private void trackAttackConfirmation(int var1) {
        EntityPlayer var2 = this.combatTracker.getTrackedTarget();
        if (var2 != null && this.hitSelectSettings.useServerAttackTime.bool()) {
            this.VMsy(var2).trackAttackConfirmation(var2.hurtTime, var1, 30, 8);
        }
    }

    private int computeAttackFlags(PlayerHitTracker var1, int var2) {
        if (this.combatTracker.getTrackedTarget() == null) {
            return 0;
        } else if (this.hitSelectSettings.disableDuringKnockback.bool() && this.isKnockbackActive()) {
            return 0;
        } else {
            int var3 = 0;
            if (this.hwrRxm(var2)) {
                var3 |= 1;
            }

            var3 |= this.computePauseFlags(var1, var2);
            if (this.isCriticalAttackReady(var1, var2)) {
                var3 |= 32;
            }

            return var3;
        }
    }

    private int computePauseFlags(PlayerHitTracker var1, int var2) {
        return var1.computeAttackTimingFlags(this.hitSelectSettings.useServerAttackTime.bool(), var2, HitSelectUtils.millisToTicks(this.hitSelectSettings.pauseDuration.number()), 10, 8, 16);
    }

    private boolean isCriticalAttackReady(PlayerHitTracker var1, int var2) {
        if (this.getCurrentMode() != HitSelectMode.CRITICALS) {
            return false;
        } else if (mc.thePlayer.onGround) {
            return false;
        } else if (this.hitSelectSettings.onlyWhileDamaged.bool() && !var1.hasTakenDamage()) {
            return false;
        } else {
            return this.hitSelectSettings.disableDuringKnockback.bool() && this.isKnockbackActive() ? false : !this.canPerformCriticalHit();
        }
    }

    private HitSelectMode getCurrentMode() {
        return HitSelectMode.fromSetting(this.hitSelectSettings.mode.choice());
    }

    private boolean hwrRxm(int var1) {
        return this.combatTracker.getLocalPlayerState().isWaitingForFirstHit(this.hitSelectSettings.waitForFirstHit.number(), this.combatTracker.getTrackedTarget() != null, var1);
    }

    private boolean canPerformCriticalHit() {
        return HitSelectUtils.isCriticalSwing(
            mc.thePlayer.fallDistance,
            mc.thePlayer.onGround,
            mc.thePlayer.isOnLadder(),
            mc.thePlayer.isInWater(),
            mc.thePlayer.isPotionActive(Potion.blindness),
            mc.thePlayer.ridingEntity != null
        );
    }

    private boolean isKnockbackActive() {
        return this.combatTracker.getLocalPlayerState().isKnockbackActive(mc.thePlayer.hurtTime);
    }

    private boolean isPauseTriggered(PlayerHitTracker var1, int var2, int var3) {
        return var1.ngDbe(var2, var3, this.hitSelectSettings.pauseDuration.number());
    }

    private void ZUbdxZ0(EntityPlayer var1, int var2) {
        if (var1 != null) {
            this.recordAttack(var1, var2);
            this.VMsy(var1).onLocalAttack(this.hitSelectSettings.useServerAttackTime.bool(), var2, 10);
        }
    }

    private boolean rollChance(double var1) {
        return HitSelectUtils.rollChancePercent(var1, Math.random());
    }

    private PlayerHitTracker VMsy(EntityPlayer var1) {
        return this.combatTracker.getOrCreatePlayerState(var1, this.hitSelectSettings.useServerAttackTime.bool());
    }

    private void qjoj() {
        this.combatTracker.resetAll();
    }

}
