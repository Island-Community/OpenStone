package stone.util.item;

import java.util.ArrayList;
import java.util.List;

public final class CategoryList {
    public static final String[] keM = {
        "@category:sword",
        "@category:block",
        "@category:stick",
        "@category:gapple",
        "@category:fireball",
        "@category:bow",
        "@category:fishing_rod",
        "@category:fish",
        "@category:snowball",
        "@category:egg",
        "@category:clock",
        "@category:enderpearl",
        "@category:water",
        "@category:lava",
        "@category:axe",
        "@category:pickaxe",
        "@category:shears",
        "@category:ladder"
    };

    private final List<List<String>> slots = new ArrayList<>(9);

    public CategoryList() {
        for (int i = 0; i < 9; i++) {
            slots.add(List.of());
        }
    }

    public List<String> getCategoriesForSlot(int slot) {
        return slot >= 0 && slot < 9 ? slots.get(slot) : List.of();
    }
}
