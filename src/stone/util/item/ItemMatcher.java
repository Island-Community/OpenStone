package stone.util.item;

import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class ItemMatcher {
    private static final ItemEntryIndex BfdVpj = new ItemEntryIndex();

    private ItemMatcher() {
    }

    public static String getStackItemId(ItemStack var0) {
        return ItemIds.getItemId(var0);
    }

    public static String getItemId(Item var0) {
        return ItemIds.getRegistryName(var0);
    }

    public static String toCanonicalItemId(String var0) {
        return ItemIds.GlVmhe(var0);
    }

    public static boolean hasMultipleVariants(Item var0) {
        String var1 = ItemIds.getRegistryName(var0);
        return var1 == null ? false : BfdVpj.getEntriesForId(var1).size() > 1;
    }

    public static boolean AkEm(List<String> var0, ItemStack var1) {
        if (var0 == null) {
            return false;
        } else {
            for (String var3 : var0) {
                if (ItemIds.matchesId(var3, var1)) {
                    return true;
                }
            }

            return false;
        }
    }

    public static boolean matches(String var0, ItemStack var1) {
        return ItemIds.matchesId(var0, var1);
    }

    public static double computeMatchScore(String var0, ItemStack var1, boolean var2, boolean var3) {
        if (!ItemIds.matchesId(var0, var1)) {
            return Double.NEGATIVE_INFINITY;
        } else {
            ItemCategory var4 = ItemCategory.from(var0);
            return var4 == null ? 0.0 : ItemCategoryScorerRegistry.scoreItem(var4, var1, var2, var3);
        }
    }
}
