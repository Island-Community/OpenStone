package stone.util.item;

import net.minecraft.item.ItemStack;

public final class ItemMatcher$1 {
    private final ItemStack itemStack;
    public final String hvDbs;
    public final String Qwi;
    public final int nfxLs;

    public ItemMatcher$1(int var2, String var3, String var4, ItemStack var5) {
        this.itemStack = var5 != null ? var5.copy() : null;
        this.hvDbs = var4;
        this.Qwi = var3 == null ? "" : var3;
        this.nfxLs = var2;
    }

    public ItemStack copyItemStack() {
        return this.itemStack != null ? this.itemStack.copy() : null;
    }
}
