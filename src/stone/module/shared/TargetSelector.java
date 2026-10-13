package stone.module.shared;

import stone.module.other.AntiBot;
import stone.util.ClientUtils;
import stone.util.rotation.RotationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public final class TargetSelector {
    private TargetSelector() {
    }

    public static EntityPlayer findCrosshairOrNearestTarget(Minecraft var0, double var1, boolean var3) {
        EntityPlayer var4 = findCrosshairTarget(var0, var1, var3);
        return var4 == null ? findNearestTarget(var0, var1, var3) : var4;
    }

    public static EntityPlayer findNearestTarget(Minecraft var0, double var1, boolean var3) {
        if (var0 != null && var0.theWorld != null) {
            EntityPlayer var4 = null;
            double var5 = Double.MAX_VALUE;

            for (EntityPlayer var8 : var0.theWorld.playerEntities) {
                if (isValidTarget(var0, var8, var3)) {
                    double var9 = irNzgN(var8);
                    if (var9 <= var1 && var9 < var5) {
                        var4 = var8;
                        var5 = var9;
                    }
                }
            }

            return var4;
        } else {
            return null;
        }
    }

    public static EntityPlayer findCrosshairTarget(Minecraft var0, double var1, boolean var3) {
        Entity var4 = var0 != null && var0.objectMouseOver != null ? var0.objectMouseOver.entityHit : null;
        return GHpv(var0, var4, var1, var3);
    }

    public static EntityPlayer GHpv(Minecraft var0, Entity var1, double var2, boolean var4) {
        if (!(var1 instanceof EntityPlayer)) {
            return null;
        } else {
            EntityPlayer var5 = (EntityPlayer)var1;
            return IegbPf(var0, var5, var2, var4) ? var5 : null;
        }
    }

    public static boolean IegbPf(Minecraft var0, EntityPlayer var1, double var2, boolean var4) {
        return isValidTarget(var0, var1, var4) && isWithinRange(var1, var2);
    }

    public static boolean isValidTarget(Minecraft var0, EntityPlayer var1, boolean var2) {
        return ClientUtils.isInWorld() && var1 != null && var0 != null && var1 != var0.thePlayer && !var1.isDead && var1.deathTime == 0
            ? TargetEligibility.isEligibleTarget(true, false, false, false, 0, ClientUtils.isFriend(var1), AntiBot.shouldHideEntity(var1), ClientUtils.isTeammate(var1), var2)
            : false;
    }

    public static boolean isWithinRange(EntityPlayer var0, double var1) {
        return var0 != null && irNzgN(var0) <= var1;
    }

    private static double irNzgN(EntityPlayer var0) {
        return RotationUtils.getDistanceSqToEntity(var0);
    }
}
