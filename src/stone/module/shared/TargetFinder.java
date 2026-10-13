package stone.module.shared;

import stone.util.IMinecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public final class TargetFinder implements IMinecraft {
    private TargetFinder() {
    }

    public static EntityPlayer findTarget(double var0) {
        return TargetSelector.findCrosshairOrNearestTarget(mc, var0, true);
    }

    public static EntityPlayer syisp(double var0, boolean var2) {
        return TargetSelector.findCrosshairOrNearestTarget(mc, var0, var2);
    }

    public static EntityPlayer findNearestTarget(double var0) {
        return TargetSelector.findNearestTarget(mc, var0, true);
    }

    public static EntityPlayer findCrosshairTarget(double var0) {
        return TargetSelector.findCrosshairTarget(mc, var0, true);
    }

    public static EntityPlayer Dhg1(double var0, boolean var2) {
        return TargetSelector.findCrosshairTarget(mc, var0, var2);
    }

    public static EntityPlayer TDBVqmH(Entity var0, double var1) {
        return TargetSelector.GHpv(mc, var0, var1, true);
    }

    public static boolean eOkw(EntityPlayer var0, double var1) {
        return TargetSelector.IegbPf(mc, var0, var1, true);
    }

    public static boolean isValidTargetInRange(EntityPlayer var0, double var1, boolean var3) {
        return TargetSelector.IegbPf(mc, var0, var1, var3);
    }

    public static boolean isValidTarget(EntityPlayer var0) {
        return TargetSelector.isValidTarget(mc, var0, true);
    }

    public static boolean isValidTargetWithTeamCheck(EntityPlayer var0, boolean var1) {
        return TargetSelector.isValidTarget(mc, var0, var1);
    }

    public static boolean isWithinRange(EntityPlayer var0, double var1) {
        return TargetSelector.isWithinRange(var0, var1);
    }
}
