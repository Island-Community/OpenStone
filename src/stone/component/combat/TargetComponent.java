package stone.component.combat;

import stone.module.other.AntiBot;
import stone.util.ClientUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;

public final class TargetComponent {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private TargetComponent() {}

    public static EntityLivingBase getNearestTarget(double range) {
        List<EntityLivingBase> list = getTargets(range);
        return list.isEmpty() ? null : list.get(0);
    }

    public static EntityLivingBase e(double range) {
        return getNearestTarget(range);
    }

    public static List<EntityLivingBase> getTargets(double range) {
        List<EntityLivingBase> result = new ArrayList<>();
        if (mc.theWorld == null || mc.thePlayer == null) {
            return result;
        }

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase living)) {
                continue;
            }
            if (living == mc.thePlayer) {
                continue;
            }
            if (living.isDead || living.getHealth() <= 0.0f) {
                continue;
            }
            if (living instanceof EntityArmorStand) {
                continue;
            }
            if (AntiBot.shouldHideEntity(living)) {
                continue;
            }
            if (living instanceof EntityPlayer player && ClientUtils.isFriend(player)) {
                continue;
            }
            double dist = mc.thePlayer.getDistanceToEntity(living);
            if (dist <= range) {
                result.add(living);
            }
        }

        result.sort(Comparator.comparingDouble(e -> mc.thePlayer.getDistanceToEntity(e)));
        return result;
    }

    public static List<EntityLivingBase> f(double range) {
        return getTargets(range);
    }
}
