package stone.module.render.blockesp;

import stone.util.block.BlockColorList;
import stone.util.block.BlockIdPatterns;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;

public final class BlockEspColorResolver {
    private BlockEspColorResolver() {
    }

    public static int resolveColor(IBlockState var0, BlockColorList var1) {
        String var2 = GUPt(var0);
        String var3 = BlockIdPatterns.stripWildcardSuffix(var2);

        for (String var5 : var1.getEntries()) {
            boolean var6 = var5.equals(var2) || var5.equals(var3);
            String var7 = BlockIdPatterns.stripWildcardSuffix(var5);
            boolean var8 = BlockIdPatterns.hasWildcardSuffix(var5) && var7 != null && var7.equals(var3);
            if (var6 || var8) {
                return var1.getBlockColor(var5);
            }
        }

        MapColor var9 = var0.getBlock().getMapColor(var0);
        return var9 == null ? -65536 : 0xFF000000 | var9.colorValue;
    }

    private static String GUPt(IBlockState var0) {
        ResourceLocation var1 = (ResourceLocation)Block.blockRegistry.getNameForObject(var0.getBlock());
        if (var1 == null) {
            return "";
        } else {
            int var2 = var0.getBlock().getMetaFromState(var0);
            return var2 == 0 ? var1.toString() : var1.toString() + ":" + var2;
        }
    }
}
