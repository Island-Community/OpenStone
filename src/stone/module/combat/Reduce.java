package stone.module.combat;

import stone.event.AttackEntityEvent;
import stone.event.ClickMouseEvent;
import stone.event.EventBus;
import stone.event.PacketSendEvent;
import stone.event.PreUpdateEvent;
import stone.event.Subscribe;
import stone.event.VelocityEvent;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.shared.TargetFinder;
import stone.util.ClientUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.client.C16PacketClientStatus.EnumState;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import org.lwjgl.input.Mouse;

public class Reduce extends Module {
    private int ZWoE4;
    private int tcx = -1;
    private boolean TKNwK;
    private boolean sentAttack;
    private boolean sentSwingAnimation;
    private boolean sentItemUse;
    private boolean sentInventoryAction;
    private boolean sentBlockDig;
    private boolean postingAttackEvent;
    private boolean performingInternalAttack;
    private Setting onlyWhilstAgainstKbStick;
    private Setting notWhilstBlocking;
    private Setting onlyWhilstHoldingWeapon;
    private Setting whilstHoldingLeft;

    public Reduce() {
        super("Reduce", "Attacks back when hit to cut the knockback", Category.COMBAT, Subcategory.ADVANCED);
        this.onlyWhilstAgainstKbStick = addBoolean("Only whilst against KB Stick", "", false);
        this.notWhilstBlocking = addBoolean("Not whilst blocking", "", false);
        this.onlyWhilstHoldingWeapon = addBoolean("Only whilst holding weapon", "", false);
        this.whilstHoldingLeft = addBoolean("Whilst holding left", "", false);
    }

    @Override
    public void onEnable(Context context) {
        this.resetState();
    }

    @Override
    public void onDisable(Context context) {
        this.resetState();
    }

    @Override
    public String suffix() {
        return "";
    }

    @Subscribe
    public void onVelocity(VelocityEvent var1) {
        if (ClientUtils.isInWorld() && !var1.cancelled()) {
            S12PacketEntityVelocity var2 = var1.packet;
            if (var2 != null && var2.getEntityID() == mc.thePlayer.getEntityId()) {
                if (!this.hasAnyConditionEnabled()) {
                    this.ZWoE4 = this.Wyh0(var2.getMotionX(), var2.getMotionZ());
                    this.tcx = -1;
                    return;
                }

                EntityLivingBase var3 = this.PeYug();
                if (!this.passesTargetConditions(var3)) {
                    this.ZWoE4 = 0;
                    this.tcx = -1;
                    return;
                }

                this.ZWoE4 = this.Wyh0(var2.getMotionX(), var2.getMotionZ());
                this.tcx = this.onlyWhilstAgainstKbStick.bool() ? var3.getEntityId() : -1;
            }
        }
    }

    @Subscribe(priority = Subscribe.Priority.LOWEST)
    public void onPacketSend(PacketSendEvent var1) {
        if (!var1.cancelled()) {
            Packet var2 = var1.getPacket();
            if (this.performingInternalAttack || !this.isCounterAttackActive() || !(var2 instanceof C0APacketAnimation) && !this.nqIvw(var2)) {
                if (!this.performingInternalAttack) {
                    if (var2 instanceof C09PacketHeldItemChange) {
                        this.TKNwK = true;
                    } else if (var2 instanceof C0APacketAnimation) {
                        this.sentSwingAnimation = true;
                    } else if (var2 instanceof C02PacketUseEntity) {
                        C02PacketUseEntity var3 = (C02PacketUseEntity)var2;
                        if (var3.getAction() == Action.ATTACK) {
                            this.sentAttack = true;
                        }
                    } else if (var2 instanceof C08PacketPlayerBlockPlacement) {
                        this.sentItemUse = true;
                    } else if (var2 instanceof C07PacketPlayerDigging) {
                        this.sentItemUse = true;
                        this.sentBlockDig = true;
                    } else if (var2 instanceof C0DPacketCloseWindow || var2 instanceof C0EPacketClickWindow) {
                        this.sentInventoryAction = true;
                    } else if (var2 instanceof C16PacketClientStatus) {
                        C16PacketClientStatus var4 = (C16PacketClientStatus)var2;
                        if (var4.getStatus() == EnumState.OPEN_INVENTORY_ACHIEVEMENT) {
                            this.sentInventoryAction = true;
                        }
                    } else if (var2 instanceof C03PacketPlayer) {
                        this.dZhz5();
                    }
                }
            } else {
                var1.setCancelled(true);
            }
        }
    }

    @Subscribe(priority = Subscribe.Priority.LOWEST)
    public void onClickMouse(ClickMouseEvent var1) {
        if (this.isCounterAttackActive()) {
            var1.setCancelled(true);
        }
    }

    @Subscribe(priority = Subscribe.Priority.LOWEST)
    public void onAttackEntity(AttackEntityEvent var1) {
        if (!this.postingAttackEvent && !this.performingInternalAttack && var1.entityPlayer == mc.thePlayer && this.isCounterAttackActive()) {
            var1.setCancelled(true);
        }
    }

    @Subscribe
    public void onPreUpdate(PreUpdateEvent var1) {
        if (!ClientUtils.isInWorld() || mc.thePlayer.isDead) {
            this.resetState();
        } else if (this.ZWoE4 > 0) {
            this.ZWoE4--;
            EntityLivingBase var2 = this.findReductionTarget();
            if (!this.isValidReductionTarget(var2)) {
                if (this.shouldAbortReduction(var2)) {
                    this.ZWoE4 = 0;
                    this.tcx = -1;
                }
            } else if (mc.getNetHandler() != null) {
                if (this.fireAttackEvent(var2)) {
                    this.performingInternalAttack = true;

                    try {
                        mc.thePlayer.swingItem();
                        mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(var2, Action.ATTACK));
                    } finally {
                        this.performingInternalAttack = false;
                    }

                    this.applyMotionReduction();
                    this.ZWoE4 = 0;
                    this.tcx = -1;
                }
            }
        }
    }

    private void applyMotionReduction() {
        mc.thePlayer.motionX *= 0.6;
        mc.thePlayer.motionZ *= 0.6;
        if (!this.gark3()) {
            mc.thePlayer.setSprinting(false);
        }
    }

    private EntityLivingBase Bsqy() {
        return TargetFinder.findCrosshairTarget(9.0);
    }

    private EntityLivingBase findReductionTarget() {
        EntityLivingBase var1 = this.Bsqy();
        if (var1 != null) {
            return var1;
        } else if (this.onlyWhilstAgainstKbStick.bool() && this.whilstHoldingLeft.bool() && this.tcx >= 0 && mc.theWorld != null) {
            Entity var2 = mc.theWorld.getEntityByID(this.tcx);
            if (!(var2 instanceof EntityPlayer)) {
                return null;
            } else {
                EntityPlayer var3 = (EntityPlayer)var2;
                return this.isHoldingKbStick(var3) && TargetFinder.isWithinRange(var3, 9.0) ? var3 : null;
            }
        } else {
            return null;
        }
    }

    private EntityLivingBase PeYug() {
        if (!this.onlyWhilstAgainstKbStick.bool()) {
            return this.Bsqy();
        } else {
            EntityPlayer var1 = TargetFinder.findCrosshairTarget(25.0);
            if (this.isHoldingKbStick(var1)) {
                return var1;
            } else {
                var1 = TargetFinder.findNearestTarget(25.0);
                return this.isHoldingKbStick(var1) ? var1 : null;
            }
        }
    }

    private boolean isValidReductionTarget(EntityLivingBase var1) {
        if (var1 == null || var1 == mc.thePlayer || var1.isDead || var1.deathTime != 0) {
            return false;
        } else if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.isDead) {
            return false;
        } else if (mc.thePlayer.isInWeb) {
            return false;
        } else if (!ClientUtils.uUwk()) {
            return false;
        } else if (!mc.thePlayer.isSprinting()) {
            return false;
        } else if (this.hasSentActionPacket(false, false, this.AbPd(), false, false, false)) {
            return false;
        } else {
            return this.onlyWhilstAgainstKbStick.bool() && this.tcx >= 0 && var1.getEntityId() != this.tcx ? false : !this.hasAnyConditionEnabled() || this.passesTargetConditions(var1);
        }
    }

    private boolean passesTargetConditions(EntityLivingBase var1) {
        if (this.onlyWhilstHoldingWeapon.bool() && !ClientUtils.isHoldingWeapon()) {
            return false;
        } else {
            return !this.passesHoldingConditions() ? false : !this.onlyWhilstAgainstKbStick.bool() || this.isHoldingKbStick(var1);
        }
    }

    private boolean passesHoldingConditions() {
        return this.notWhilstBlocking.bool() && this.isBlocking() ? false : !this.whilstHoldingLeft.bool() || Mouse.isButtonDown(0);
    }

    private boolean shouldAbortReduction(EntityLivingBase var1) {
        if (!this.hasAnyConditionEnabled()) {
            return false;
        } else {
            return !this.passesTargetConditions(var1) ? true : this.onlyWhilstAgainstKbStick.bool() && var1.getEntityId() != this.tcx;
        }
    }

    private boolean hasAnyConditionEnabled() {
        return this.onlyWhilstAgainstKbStick.bool() || this.notWhilstBlocking.bool() || this.onlyWhilstHoldingWeapon.bool() || this.whilstHoldingLeft.bool();
    }

    private boolean AbPd() {
        return Mouse.isButtonDown(0) && !this.sentAttack && !this.isCounterAttackActive();
    }

    private boolean gark3() {
        return this.onlyWhilstAgainstKbStick.bool() && this.whilstHoldingLeft.bool() && Mouse.isButtonDown(0);
    }

    private boolean fireAttackEvent(EntityLivingBase var1) {
        this.postingAttackEvent = true;

        AttackEntityEvent var2;
        try {
            var2 = EventBus.post(new AttackEntityEvent(var1, mc.thePlayer, true));
        } finally {
            this.postingAttackEvent = false;
        }

        return !var2.cancelled();
    }

    private boolean isCounterAttackActive() {
        if (!ClientUtils.isInWorld() || !this.onlyWhilstAgainstKbStick.bool() || !this.whilstHoldingLeft.bool() || !Mouse.isButtonDown(0)) {
            return false;
        } else if (!this.passesHoldingConditions()) {
            return false;
        } else {
            EntityLivingBase var1 = this.findKbStickTarget();
            return var1 != null && this.passesTargetConditions(var1);
        }
    }

    private boolean isBlocking() {
        return mc.thePlayer.isBlocking() || Mouse.isButtonDown(1) && ClientUtils.CqWuiK();
    }

    private EntityLivingBase findKbStickTarget() {
        EntityPlayer var1 = TargetFinder.findCrosshairTarget(20.25);
        if (this.isHoldingKbStick(var1)) {
            return var1;
        } else {
            var1 = TargetFinder.findNearestTarget(20.25);
            return this.isHoldingKbStick(var1) ? var1 : null;
        }
    }

    private boolean nqIvw(Packet<?> var1) {
        if (!(var1 instanceof C02PacketUseEntity)) {
            return false;
        } else {
            C02PacketUseEntity var2 = (C02PacketUseEntity)var1;
            return var2.getAction() == Action.ATTACK;
        }
    }

    private boolean isHoldingKbStick(EntityLivingBase var1) {
        if (var1 instanceof EntityPlayer && TargetFinder.isWithinRange((EntityPlayer)var1, 25.0)) {
            ItemStack var2 = var1.getHeldItem();
            return var2 != null && var2.getItem() == Items.stick;
        } else {
            return false;
        }
    }

    private int Wyh0(int var1, int var2) {
        double var3 = Math.hypot(var1, var2);
        if (var3 <= 500.0) {
            return 3;
        } else if (var3 <= 1000.0) {
            return 4;
        } else if (var3 <= 2000.0) {
            return 4;
        } else if (var3 <= 3000.0) {
            return 5;
        } else if (var3 <= 4000.0) {
            return 6;
        } else if (var3 <= 5000.0) {
            return 6;
        } else if (var3 <= 6000.0) {
            return 7;
        } else if (var3 <= 7000.0) {
            return 7;
        } else if (var3 <= 8000.0) {
            return 8;
        } else {
            return var3 <= 9000.0 ? 8 : 9;
        }
    }

    private boolean hasSentActionPacket(boolean var1, boolean var2, boolean var3, boolean var4, boolean var5, boolean var6) {
        if (this.TKNwK && !var1) {
            return true;
        } else if (this.sentAttack && !var2) {
            return true;
        } else if (this.sentSwingAnimation && !var3) {
            return true;
        } else if (this.sentItemUse && !var4) {
            return true;
        } else {
            return this.sentInventoryAction && !var5 ? true : this.sentBlockDig && !var6;
        }
    }

    private void dZhz5() {
        this.TKNwK = false;
        this.sentSwingAnimation = false;
        this.sentAttack = false;
        this.sentItemUse = false;
        this.sentInventoryAction = false;
        this.sentBlockDig = false;
    }

    private void resetState() {
        this.ZWoE4 = 0;
        this.tcx = -1;
        this.dZhz5();
    }

}
