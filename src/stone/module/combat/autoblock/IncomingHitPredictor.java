package stone.module.combat.autoblock;

import stone.module.shared.TargetFinder;
import stone.util.rotation.RotationUtils;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.Vec3;

public final class IncomingHitPredictor {
    private static final double reJ = Math.cos(Math.toRadians(30.0));
    private final Map<Integer, IncomingHitPredictor$2> ZdX = new HashMap<>();

    public void DkCma(int var1, long var2) {
        this.ZdX.put(var1, new IncomingHitPredictor$2(var2));
    }

    public IncomingHitPredictor$3 selectBestTarget(EntityPlayer var1, Iterable<EntityPlayer> var2, double var3, boolean var5, long var6) {
        this.ozbG(var6);
        IncomingHitPredictor$3 var8 = null;
        double var9 = var3 * var3;

        for (EntityPlayer var12 : var2) {
            IncomingHitPredictor$2 var13 = this.ZdX.get(var12.getEntityId());
            if (var13 != null && !IncomingHitPredictor$2.isInvalidated(var13)) {
                IncomingHitPredictor$3 var14 = this.MUZs(var12, var1, var13, var9, var6);
                if (var14 != null
                    && TargetFinder.isValidTargetWithTeamCheck(var12, var5)
                    && (
                        var8 == null
                            || var14.score > var8.score
                            || var14.score == var8.score && var14.distanceSquared < var8.distanceSquared
                            || var14.score == var8.score && var14.distanceSquared == var8.distanceSquared && var14.recordedTimeMillis > var8.recordedTimeMillis
                    )) {
                    var8 = var14;
                }
            }
        }

        return var8;
    }

    public IncomingHitPredictor$3 evaluateTargetById(int var1, EntityPlayer var2, double var3, boolean var5, long var6) {
        this.ozbG(var6);
        if (var2 != null && var2.worldObj != null) {
            Entity var8 = var2.worldObj.getEntityByID(var1);
            if (!(var8 instanceof EntityPlayer)) {
                return null;
            } else {
                EntityPlayer var9 = (EntityPlayer)var8;
                if (!TargetFinder.isValidTargetWithTeamCheck(var9, var5)) {
                    return null;
                } else {
                    IncomingHitPredictor$2 var10 = this.ZdX.get(var1);
                    return var10 == null ? null : this.MUZs(var9, var2, var10, var3 * var3, var6);
                }
            }
        } else {
            return null;
        }
    }

    public void invalidateTarget(int var1) {
        IncomingHitPredictor$2 var2 = this.ZdX.get(var1);
        if (var2 != null) {
            IncomingHitPredictor$2.PITAi(var2, true);
        }
    }

    public void GWxd() {
        this.ZdX.clear();
    }

    private IncomingHitPredictor$3 MUZs(EntityPlayer var1, EntityPlayer var2, IncomingHitPredictor$2 var3, double var4, long var6) {
        if (var6 - IncomingHitPredictor$2.getRecordedTime(var3) > 800L) {
            return null;
        } else {
            double var8 = RotationUtils.getDistanceSqToEntity(var1);
            if (!(var8 > var4) && var1.canEntityBeSeen(var2) && isLookingAtTarget(var1, var2)) {
                int var10 = 0;
                double var11 = Math.sqrt(var8);
                if (var11 <= 3.25) {
                    var10 += 3;
                }

                IncomingHitPredictor$1 var13 = computeApproachMetrics(var1, var2);
                if (IncomingHitPredictor$1.getApproachDistance(var13) <= 3.25) {
                    var10 += 2;
                }

                if (IncomingHitPredictor$1.VGdeC(var13) >= 0.05) {
                    var10++;
                }

                if (isHoldingWeapon(var1)) {
                    var10++;
                }

                if (var1.isSprinting() && IncomingHitPredictor$1.isTargetMovingAway(var13)) {
                    var10++;
                }

                return var10 >= 3 ? new IncomingHitPredictor$3(var1.getEntityId(), IncomingHitPredictor$2.getRecordedTime(var3), var10, var8) : null;
            } else {
                return null;
            }
        }
    }

    private void ozbG(long var1) {
        Iterator var3 = this.ZdX.entrySet().iterator();

        while (var3.hasNext()) {
            IncomingHitPredictor$2 var4 = (IncomingHitPredictor$2)((Entry)var3.next()).getValue();
            if (var1 - IncomingHitPredictor$2.getRecordedTime(var4) > 800L) {
                var3.remove();
            }
        }
    }

    private static boolean isLookingAtTarget(EntityPlayer var0, EntityPlayer var1) {
        Vec3 var2 = var0.getPositionEyes(1.0F);
        Vec3 var3 = var1.getPositionEyes(1.0F);
        Vec3 var4 = var3.subtract(var2);
        double var5 = var4.lengthVector();
        if (var5 < 0.001) {
            return true;
        } else {
            double var7 = Math.toRadians(var0.rotationYawHead);
            double var9 = Math.toRadians(var0.rotationPitch);
            Vec3 var11 = new Vec3(-Math.sin(var7) * Math.cos(var9), -Math.sin(var9), Math.cos(var7) * Math.cos(var9));
            return var11.dotProduct(var4) / var5 >= reJ;
        }
    }

    private static IncomingHitPredictor$1 computeApproachMetrics(EntityPlayer var0, EntityPlayer var1) {
        double var2 = var1.posX - var0.posX;
        double var4 = var1.posY + var1.getEyeHeight() - var0.posY - var0.getEyeHeight();
        double var6 = var1.posZ - var0.posZ;
        double var8 = var1.posX - var1.prevPosX;
        double var10 = var1.posY - var1.prevPosY;
        double var12 = var1.posZ - var1.prevPosZ;
        double var14 = var0.posX - var0.prevPosX;
        double var16 = var0.posY - var0.prevPosY;
        double var18 = var0.posZ - var0.prevPosZ;
        double var20 = var8 - var14;
        double var22 = var10 - var16;
        double var24 = var12 - var18;
        double var26 = Math.sqrt(var2 * var2 + var4 * var4 + var6 * var6);
        double var28 = var2 + var20;
        double var30 = var4 + var22;
        double var32 = var6 + var24;
        double var34 = Math.sqrt(var28 * var28 + var30 * var30 + var32 * var32);
        double var36 = var2 + var20 * 2.0;
        double var38 = var4 + var22 * 2.0;
        double var40 = var6 + var24 * 2.0;
        double var42 = (var1.width + var0.width) * 0.5;
        double var44 = Math.max(0.0, Math.sqrt(var36 * var36 + var38 * var38 + var40 * var40) - var42);
        double var46 = Math.sqrt(var2 * var2 + var6 * var6);
        boolean var48 = var46 < 0.001 || (var14 * var2 + var18 * var6) / var46 >= 0.05;
        return new IncomingHitPredictor$1(var44, var26 - var34, var48);
    }

    private static boolean isHoldingWeapon(EntityPlayer var0) {
        ItemStack var1 = var0.getHeldItem();
        if (var1 == null) {
            return false;
        } else {
            Item var2 = var1.getItem();
            return var2 instanceof ItemSword || var2 instanceof ItemAxe;
        }
    }
}
