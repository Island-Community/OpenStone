package stone.module.combat.shared;

import stone.util.block.BlockList;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;

public final class BreakableBlockWhitelist {
    private static final String[] EtnyG = new String[]{
        "minecraft:planks:*",
        "minecraft:log:*",
        "minecraft:log2:*",
        "minecraft:clay",
        "minecraft:end_stone",
        "minecraft:glass",
        "minecraft:wool:*",
        "minecraft:packed_ice",
        "minecraft:ladder",
        "minecraft:obsidian"
    };

    private BreakableBlockWhitelist() {
    }

    public static void TdpPo7(BlockList var0) {
        for (String var4 : EtnyG) {
            var0.addEntry(var4);
        }
    }

    public static boolean isBlockWhitelisted(Minecraft var0, BlockPos var1, BlockList var2) {
        if (var0 != null && var0.theWorld != null && var1 != null && var2 != null) {
            IBlockState var3 = var0.theWorld.getBlockState(var1);
            Block var4 = var3.getBlock();
            if (var4 != null && Block.blockRegistry.getNameForObject(var4) != null) {
                String var5 = ((ResourceLocation)Block.blockRegistry.getNameForObject(var4)).toString();
                int var6 = var4.getMetaFromState(var3);
                String var7 = var6 == 0 ? var5 : var5 + ":" + var6;
                return var2.containsEntry(var7) || var2.containsEntry(var5);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
