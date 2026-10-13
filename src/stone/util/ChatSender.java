package stone.util;

import stone.game.Game;
import net.minecraft.client.Minecraft;

public final class ChatSender {
    private ChatSender() {
    }

    public static String formatTitle(String var0) {
        return "§b§l" + var0 + "§r";
    }

    public static void HARd(Minecraft var0, String var1) {
        if (Mmb4(var0)) {
            Game.printChat(TextUtils.QJdK(var1));
        }
    }

    public static void daPdo(Minecraft var0, String var1, String var2) {
        if (Mmb4(var0)) {
            Game.printChat(formatTitle(var1) + TextUtils.QJdK(var2));
        }
    }

    public static void sendDebugMessage(Minecraft var0, String var1) {
        if (Mmb4(var0)) {
            Game.printChat(TextUtils.QJdK("&8[debug]&r ") + var1);
        }
    }

    private static boolean Mmb4(Minecraft var0) {
        return var0.thePlayer != null && var0.theWorld != null;
    }
}
