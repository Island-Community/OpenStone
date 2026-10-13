package stone.util.block;

import net.minecraft.client.Minecraft;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;

public final class LoadedChunks {
    private LoadedChunks() {
    }

    public static void forEach(Minecraft var0, LoadedChunks$0 var1) {
        if (var0.theWorld != null && var0.thePlayer != null) {
            int var2 = var0.gameSettings.renderDistanceChunks;
            int var3 = (int)var0.thePlayer.posX >> 4;
            int var4 = (int)var0.thePlayer.posZ >> 4;

            for (int var5 = var3 - var2; var5 <= var3 + var2; var5++) {
                for (int var6 = var4 - var2; var6 <= var4 + var2; var6++) {
                    Chunk var7 = var0.theWorld.getChunkFromChunkCoords(var5, var6);
                    if (var7 != null && !(var7 instanceof EmptyChunk)) {
                        var1.onChunkLoaded(var5, var6);
                    }
                }
            }
        }
    }
}
