package stone.util.item;

import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;

public final class ToolUtils {
    private ToolUtils() {
    }

    public static double Mbr2(ItemStack var0, Block var1, boolean var2) {
        return var0 != null && var1 != null
            ? ItemScoreUtils.getToolScore(
                var0.getStrVsBlock(var1),
                WeaponDamage.getEnchantmentLevel(var0, Enchantment.efficiency),
                var0.canHarvestBlock(var1),
                !var1.getMaterial().isToolNotRequired(),
                DurabilityUtils.xuZl(var0, var2)
            )
            : 0.0;
    }

    public static double getHoeTotalDurability(ItemStack var0, boolean var1) {
        return var0 != null && var0.getItem() instanceof ItemHoe ? var0.getMaxDamage() + DurabilityUtils.xuZl(var0, var1) : 0.0;
    }
}
