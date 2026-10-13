package stone.module.combat.attributeswap;

import stone.util.item.ToolUtils;
import stone.util.item.WeaponDamage;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

public final class ItemScorer {
    private ItemScorer() {
    }

    public static double getTotalMeleeDamage(ItemStack var0) {
        return KbTm2(var0, true);
    }

    public static double KbTm2(ItemStack var0, boolean var1) {
        return WeaponDamage.getTotalMeleeDamage(var0, var1);
    }

    public static double scoreBowDamage(ItemStack var0, boolean var1) {
        return WeaponDamage.TALTByo(var0, var1);
    }

    public static double scoreBestBreakingTool(ItemStack var0, boolean var1) {
        return Math.max(Math.max(scoreWoodBreaking(var0, var1), scoreStoneBreaking(var0, var1)), Math.max(Math.max(scoreDirtBreaking(var0, var1), scoreHoe(var0, var1)), scoreWebBreaking(var0, var1)));
    }

    public static double scoreWoodBreaking(ItemStack var0, boolean var1) {
        return Aqkw(var0, Blocks.log, var1);
    }

    public static double scoreStoneBreaking(ItemStack var0, boolean var1) {
        return Aqkw(var0, Blocks.stone, var1);
    }

    public static double scoreDirtBreaking(ItemStack var0, boolean var1) {
        return Aqkw(var0, Blocks.dirt, var1);
    }

    public static double scoreHoe(ItemStack var0, boolean var1) {
        return ToolUtils.getHoeTotalDurability(var0, var1);
    }

    public static double scoreWebBreaking(ItemStack var0, boolean var1) {
        return Aqkw(var0, Blocks.web, var1);
    }

    public static double Aqkw(ItemStack var0, Block var1, boolean var2) {
        return ToolUtils.Mbr2(var0, var1, var2);
    }

    public static double getPlaceableBlockScore(ItemStack var0) {
        return ToolScorer.scoreBlockStack(var0);
    }

    public static double getStickScore(ItemStack var0) {
        return ToolScorer.scoreStick(var0);
    }

    public static double scoreBowEnchantments(ItemStack var0, boolean var1) {
        return ToolScorer.scoreBow(var0, var1);
    }

    public static double getStackSize(ItemStack var0) {
        return var0 == null ? 0.0 : var0.stackSize;
    }

    public static double scoreFishingRod(ItemStack var0, boolean var1) {
        return ToolScorer.scoreFishingRod(var0, var1);
    }

    public static double ZGxM(ItemStack var0) {
        return ToolScorer.scoreFish(var0);
    }

    public static double scoreAxe(ItemStack var0, boolean var1) {
        return ToolScorer.scoreAxe(var0, var1);
    }

    public static double scorePickaxe(ItemStack var0, boolean var1) {
        return ToolScorer.scorePickaxe(var0, var1);
    }

    public static double scoreShears(ItemStack var0, boolean var1) {
        return ToolScorer.scoreShears(var0, var1);
    }
}
