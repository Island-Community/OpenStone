package stone.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.ContainerPlayer;
import org.lwjgl.input.Keyboard;

public final class GameStateUtils {
    private GameStateUtils() {
    }

    public static boolean isInWorld(Minecraft var0) {
        return var0.thePlayer != null && var0.theWorld != null;
    }

    public static boolean isInGameWithFocus(Minecraft var0) {
        return var0.currentScreen == null && var0.inGameHasFocus;
    }

    public static boolean isOnHypixel(Minecraft var0) {
        return !var0.isSingleplayer() && var0.getCurrentServerData() != null && var0.getCurrentServerData().serverIP.contains("hypixel.net");
    }

    public static boolean isPlayerInventoryOpen(Minecraft var0) {
        return !isInWorld(var0) ? false : var0.currentScreen instanceof GuiInventory && var0.thePlayer.inventoryContainer instanceof ContainerPlayer;
    }

    public static boolean yrvB(Minecraft var0) {
        return Keyboard.isKeyDown(var0.gameSettings.keyBindJump.getKeyCode());
    }
}
