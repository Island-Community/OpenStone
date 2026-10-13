package stone.util.rotation;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public final class EntityAngles {
    private EntityAngles() {
    }

    public static float getYawTo(EntityPlayer var0, Entity var1) {
        return AngleUtils.getYawToPoint(var0.posX, var0.posZ, var1.posX, var1.posZ);
    }

    public static float getPitchTo(EntityPlayer var0, Entity var1) {
        double var2 = var1.posX - var0.posX;
        double var4 = var1.posZ - var0.posZ;
        double var6 = var1.posY + var1.getEyeHeight() * 0.5F - (var0.posY + var0.getEyeHeight());
        return (float)(-Math.toDegrees(Math.atan2(var6, Math.sqrt(var2 * var2 + var4 * var4))));
    }

    public static double wrapAngleDifference(float var0, float var1) {
        double var2 = (var0 - var1) % 360.0;
        return (var2 + 540.0) % 360.0 - 180.0;
    }
}
