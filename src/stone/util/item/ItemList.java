package stone.util.item;

import stone.util.block.BlockList;
import java.util.List;
import net.minecraft.item.ItemStack;

public class ItemList extends BlockList {
    public List<String> getItems() {
        return getEntries();
    }

    public void addItem(String id) {
        addEntry(id);
    }

    public boolean matches(ItemStack stack) {
        return ItemMatcher.AkEm(getItems(), stack);
    }
}
