package stone.module.combat;

import stone.event.AttackEntityEvent;
import stone.event.ClickMouseEvent;
import stone.event.DisconnectEvent;
import stone.event.LoadWorldEvent;
import stone.event.PacketSendEvent;
import stone.event.PlayerAttackEvent;
import stone.event.Subscribe;
import stone.event.TickStartEvent;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.combat.autofeed.DamageCalculator;
import stone.util.ClientUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class AutoFeed extends Module {
    private final Setting minimumHealth;
    private final Setting safetyMargin;
    private final Setting onlyInBedwars;
    private final Setting onlyWhilstTeammateNear;
    private final Map<Integer, AutoFeed$2> healthTrackers = new HashMap<>();
    private final Map<Integer, AutoFeed$1> rrGx = new HashMap<>();

    public AutoFeed() {
        super("AutoFeed", "Stops attacks that would finish an enemy", Category.COMBAT, Subcategory.ADVANCED);
        this.minimumHealth = addNumber("Minimum health", "", 1.0F, 0.5F, 10.0F, 1);
        this.safetyMargin = addNumber("Safety margin", "", 0.5F, 0.0F, 3.0F, 1);
        this.onlyInBedwars = addBoolean("Only in Bedwars", "", true);
        this.onlyWhilstTeammateNear = addBoolean("Only whilst teammate near", "", true);
    }

    @Override
    public String suffix() {
        return formatTrimmedDouble(this.minimumHealth.number()) + " HP";
    }

    @Override
    public void onEnable(Context context) {
        this.clearTrackingData();
    }

    @Override
    public void onDisable(Context context) {
        this.clearTrackingData();
    }

    @Subscribe(priority = Subscribe.Priority.HIGHEST)
    public void onClickMouse(ClickMouseEvent var1) {
        Entity var2 = var1.movingObjectPosition == null ? null : var1.movingObjectPosition.entityHit;
        if (var2 instanceof EntityPlayer && this.qImi((EntityPlayer)var2)) {
            var1.setCancelled(true);
        }
    }

    @Subscribe(priority = Subscribe.Priority.HIGHEST)
    public void preventLethalAttack(AttackEntityEvent var1) {
        if (var1.entityPlayer == mc.thePlayer && var1.entity instanceof EntityPlayer && this.qImi((EntityPlayer)var1.entity)) {
            var1.setCancelled(true);
        }
    }

    @Subscribe(priority = Subscribe.Priority.LOWEST)
    public void onAttackEntity(AttackEntityEvent var1) {
        if (!var1.cancelled() && var1.entityPlayer == mc.thePlayer && var1.entity instanceof EntityPlayer) {
            this.recordAttackDamage((EntityPlayer)var1.entity);
        }
    }

    @Subscribe(priority = Subscribe.Priority.HIGHEST)
    public void onPlayerAttack(PlayerAttackEvent var1) {
        if (var1.entityPlayer == mc.thePlayer && var1.entity instanceof EntityPlayer && this.qImi((EntityPlayer)var1.entity)) {
            var1.setCancelled(true);
        }
    }

    @Subscribe(priority = Subscribe.Priority.HIGHEST)
    public void preventLethalAttackPacket(PacketSendEvent var1) {
        if (ClientUtils.isInWorld()) {
            Packet var2 = var1.getPacket();
            if (var2 instanceof C02PacketUseEntity) {
                C02PacketUseEntity var3 = (C02PacketUseEntity)var2;
                if (var3.getAction() != Action.ATTACK) {
                    return;
                }

                Entity var4 = var3.getEntityFromWorld(mc.theWorld);
                if (var4 instanceof EntityPlayer && this.qImi((EntityPlayer)var4)) {
                    var1.setCancelled(true);
                }
            } else if (var2 instanceof C0APacketAnimation) {
                EntityPlayer var5 = this.CKAiU();
                if (var5 != null && this.qImi(var5)) {
                    var1.setCancelled(true);
                }
            }
        }
    }

    @Subscribe(priority = Subscribe.Priority.LOWEST)
    public void onPacketSend(PacketSendEvent var1) {
        if (!var1.cancelled() && ClientUtils.isInWorld() && var1.getPacket() instanceof C02PacketUseEntity) {
            C02PacketUseEntity var2 = (C02PacketUseEntity)var1.getPacket();
            if (var2.getAction() == Action.ATTACK) {
                Entity var3 = var2.getEntityFromWorld(mc.theWorld);
                if (var3 instanceof EntityPlayer) {
                    this.recordAttackDamage((EntityPlayer)var3);
                }
            }
        }
    }

    @Subscribe
    public void onTickStart(TickStartEvent var1) {
        this.updateHealthTrackers();
    }

    @Subscribe
    public void onLoadWorld(LoadWorldEvent var1) {
        this.clearTrackingData();
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent var1) {
        this.clearTrackingData();
    }

    public boolean wouldCancelAttack(EntityPlayer var1) {
        return this.enabled() && this.qImi(var1);
    }

    private boolean qImi(EntityPlayer var1) {
        if (this.areConditionsMet() && this.isValidEnemy(var1)) {
            float var2 = DamageCalculator.getAttackDamage(mc.thePlayer, var1);
            float var3 = this.MFJVkwW(var1, var2);
            return DamageCalculator.isLethalDamage(var1.getHealth(), var1.getAbsorptionAmount(), var3, (float)this.minimumHealth.number(), (float)this.safetyMargin.number());
        } else {
            return false;
        }
    }

    private float MFJVkwW(EntityPlayer var1, float var2) {
        if (ClientUtils.getBedWarsBoardType() == 2) {
            AutoFeed$2 var3 = this.healthTrackers.get(var1.getEntityId());
            long var4 = System.currentTimeMillis();
            if (var3 != null && AutoFeed$2.getObservedDamageRatio(var3) > 0.0F && var4 - AutoFeed$2.getLastDamageObservationTime(var3) <= 15000L && AutoFeed$2.getSampleArmorPoints(var3) == var1.getTotalArmorValue()) {
                return DamageCalculator.applyObservedDamageRatio(var2, AutoFeed$2.getObservedDamageRatio(var3));
            }
        }

        return DamageCalculator.applyDamageReduction(var2, var1);
    }

    private boolean areConditionsMet() {
        if (ClientUtils.isInWorld() && !mc.thePlayer.isDead) {
            return this.onlyInBedwars.bool() && ClientUtils.getBedWarsBoardType() != 2 ? false : !this.onlyWhilstTeammateNear.bool() || this.isTeammateNearby();
        } else {
            return false;
        }
    }

    private boolean isTeammateNearby() {
        for (EntityPlayer var2 : mc.theWorld.playerEntities) {
            if (var2 != mc.thePlayer && !var2.isDead && var2.deathTime == 0 && ClientUtils.isTeammate(var2) && mc.thePlayer.getDistanceSqToEntity(var2) <= 400.0) {
                return true;
            }
        }

        return false;
    }

    private boolean isValidEnemy(EntityPlayer var1) {
        return var1 != null && var1 != mc.thePlayer && !var1.isDead && var1.deathTime == 0 && !ClientUtils.isTeammate(var1);
    }

    private EntityPlayer CKAiU() {
        MovingObjectPosition var1 = mc.objectMouseOver;
        return var1 != null && var1.typeOfHit == MovingObjectType.ENTITY && var1.entityHit instanceof EntityPlayer ? (EntityPlayer)var1.entityHit : null;
    }

    private void recordAttackDamage(EntityPlayer var1) {
        if (this.areConditionsMet() && this.isValidEnemy(var1)) {
            long var2 = System.currentTimeMillis();
            AutoFeed$1 var4 = this.rrGx.get(var1.getEntityId());
            if (var4 == null || var2 - AutoFeed$1.getAttackTime(var4) >= 100L) {
                float var5 = DamageCalculator.getAttackDamage(mc.thePlayer, var1);
                if (var5 > 0.0F) {
                    this.rrGx.put(var1.getEntityId(), new AutoFeed$1(var5, var2));
                }
            }
        }
    }

    private void updateHealthTrackers() {
        if (!ClientUtils.isInWorld()) {
            this.clearTrackingData();
        } else {
            long var1 = System.currentTimeMillis();
            HashSet var3 = new HashSet();

            for (EntityPlayer var5 : mc.theWorld.playerEntities) {
                if (var5 != mc.thePlayer) {
                    int var6 = var5.getEntityId();
                    var3.add(var6);
                    float var7 = var5.getHealth() + var5.getAbsorptionAmount();
                    AutoFeed$2 var8 = this.healthTrackers.get(var6);
                    if (var8 == null) {
                        this.healthTrackers.put(var6, new AutoFeed$2(var7));
                    } else {
                        float var9 = AutoFeed$2.TBJf18(var8) - var7;
                        AutoFeed$1 var10 = this.rrGx.get(var6);
                        if (var9 > 0.01F && var10 != null && var1 - AutoFeed$1.getAttackTime(var10) <= 1500L && AutoFeed$1.getExpectedDamage(var10) > 0.0F) {
                            AutoFeed$2.setObservedDamageRatio(var8, Math.min(1.0F, var9 / AutoFeed$1.getExpectedDamage(var10)));
                            AutoFeed$2.setLastDamageObservationTime(var8, var1);
                            AutoFeed$2.kcJs(var8, var5.getTotalArmorValue());
                            this.rrGx.remove(var6);
                        }

                        AutoFeed$2.setPreviousHealthTotal(var8, var7);
                    }
                }
            }

            Iterator var11 = this.healthTrackers.entrySet().iterator();

            while (var11.hasNext()) {
                if (!var3.contains(((Entry)var11.next()).getKey())) {
                    var11.remove();
                }
            }

            Iterator var12 = this.rrGx.entrySet().iterator();

            while (var12.hasNext()) {
                Entry var13 = (Entry)var12.next();
                if (!var3.contains(var13.getKey()) || var1 - AutoFeed$1.getAttackTime((AutoFeed$1)var13.getValue()) > 1500L) {
                    var12.remove();
                }
            }
        }
    }

    private void clearTrackingData() {
        this.healthTrackers.clear();
        this.rrGx.clear();
    }

    private static String formatTrimmedDouble(double var0) {
        return var0 == Math.rint(var0) ? Integer.toString((int)var0) : Double.toString(var0);
    }
}
