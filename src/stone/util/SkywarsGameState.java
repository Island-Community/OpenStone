package stone.util;

import stone.event.ChatReceivedEvent;
import stone.event.LoadWorldEvent;
import stone.event.Subscribe;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class SkywarsGameState implements IMinecraft {
    private static final SkywarsGameState skywarsGameState = new SkywarsGameState();
    private static boolean gameActive;
    private static long timerDeadlineMillis;

    private SkywarsGameState() {
    }

    public static SkywarsGameState YFaX() {
        return skywarsGameState;
    }

    public static boolean JYXs() {
        return gameActive;
    }

    public static boolean isFightInProgress() {
        return gameActive && !hasPreGameHotbarItems();
    }

    public static boolean isTimerElapsed(long var0) {
        return isFightInProgress() && timerDeadlineMillis > 0L && var0 >= timerDeadlineMillis;
    }

    public static void PqEeqyh(long var0) {
        timerDeadlineMillis = var0 + 40000L;
    }

    public static boolean hasPreGameHotbarItems() {
        return mc.thePlayer != null && mc.thePlayer.inventory != null
            ? isItemInSlot(0, Items.compass) && isItemInSlot(2, Items.emerald) && isItemInSlot(8, Items.nether_star)
            : false;
    }

    private static boolean isItemInSlot(int var0, Item var1) {
        ItemStack var2 = mc.thePlayer.inventory.getStackInSlot(var0);
        return var2 != null && var2.getItem() == var1;
    }

    @Subscribe
    public void onChatReceived(ChatReceivedEvent var1) {
        if (var1.component != null) {
            String var2 = ClientUtils.AOAtn(var1.component.getUnformattedText());
            if (var2 != null) {
                if (var2.contains("Cages opened! FIGHT!")) {
                    gameActive = true;
                    timerDeadlineMillis = System.currentTimeMillis() + 13000L;
                } else if (gameActive && var2.contains("You used your Echo ability!")) {
                    timerDeadlineMillis = System.currentTimeMillis() + 40000L;
                }
            }
        }
    }

    @Subscribe
    public void onLoadWorld(LoadWorldEvent var1) {
        gameActive = false;
        timerDeadlineMillis = 0L;
    }
}
