package stone.module.minigames;

import stone.event.ChatReceivedEvent;
import stone.event.EntityJoinWorldEvent;
import stone.event.Phase;
import stone.event.Subscribe;
import stone.event.TickEndEvent;
import stone.util.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;

public class BedwarsSpawnTracker {
    private static BlockPos blockPos;
    private static boolean shouldCaptureSpawnPosition;
    private static boolean respawnPending;
    private static long deathTimeMillis;

    public static BlockPos getSpawnBlockPos() {
        return blockPos;
    }

    @Subscribe
    public void onChatReceived(ChatReceivedEvent var1) {
        if (ClientUtils.isInWorld()) {
            String var2 = ClientUtils.AOAtn(var1.component.getUnformattedText());
            if (var2.startsWith(" ") && var2.contains("Protect your bed and destroy the enemy beds.")) {
                shouldCaptureSpawnPosition = true;
                respawnPending = false;
            } else if (var2.equals("You will respawn because you still have a bed!")) {
                respawnPending = true;
                deathTimeMillis = System.currentTimeMillis();
            } else if (var2.equals("You have respawned!") && respawnPending && ClientUtils.LvhY(System.currentTimeMillis(), deathTimeMillis) <= 12000L) {
                shouldCaptureSpawnPosition = true;
                respawnPending = false;
            }
        }
    }

    @Subscribe
    public void onTickEnd(TickEndEvent var1) {
        if (var1.phase == Phase.END && ClientUtils.isInWorld()) {
            if (shouldCaptureSpawnPosition && ClientUtils.getBedWarsBoardType() == 2) {
                blockPos = Minecraft.getMinecraft().thePlayer.getPosition();
                shouldCaptureSpawnPosition = false;
            }
        }
    }

    @Subscribe
    public void onEntityJoinWorld(EntityJoinWorldEvent var1) {
        if (var1.entity == Minecraft.getMinecraft().thePlayer) {
            blockPos = null;
            shouldCaptureSpawnPosition = false;
            respawnPending = false;
            deathTimeMillis = 0L;
        }
    }
}
