package stone.util.team;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.scoreboard.ScorePlayerTeam;

public final class TeamUtils {
    private TeamUtils() {
    }

    public static boolean isTeammate(Minecraft var0, Entity var1) {
        return var1 instanceof EntityLivingBase && var0.thePlayer != null && isTeammateByDisplayName(var0, (EntityLivingBase)var1);
    }

    private static boolean isTeammateByDisplayName(Minecraft var0, EntityLivingBase var1) {
        try {
            if (var0.thePlayer.isOnSameTeam(var1)) {
                return true;
            } else {
                String var2 = var1.getDisplayName().getUnformattedText().substring(0, 2);
                String var3 = var0.thePlayer.getDisplayName().getUnformattedText();
                return var3.startsWith(var2) || getOwnFormattedName(var0).startsWith(var2);
            }
        } catch (Exception var4) {
            return false;
        }
    }

    public static String getOwnFormattedName(Minecraft var0) {
        try {
            NetworkPlayerInfo var1 = var0.getNetHandler().getPlayerInfo(var0.thePlayer.getUniqueID());
            return ScorePlayerTeam.formatPlayerName(var1.getPlayerTeam(), var1.getGameProfile().getName());
        } catch (Exception var2) {
            return "";
        }
    }
}
