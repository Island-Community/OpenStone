package stone.util.item;

public final class ItemColorList extends ItemList {
    public int getPrimaryColor(String id) {
        return colors(id)[0];
    }

    public int getSecondaryColor(String id) {
        return colors(id)[1];
    }

    private static int[] colors(String id) {
        return switch (id) {
            case "minecraft:gold_ingot" -> new int[]{0xFFFFD779, 0xFFFFF0A8};
            case "minecraft:diamond" -> new int[]{0xFF62F5FF, 0xFFFFFFFF};
            case "minecraft:emerald" -> new int[]{0xFF2EF56F, 0xFFB8FFD0};
            case "minecraft:iron_ingot" -> new int[]{0xFFFFFFFF, 0xFFCFCFCF};
            default -> new int[]{0xFFFFFFFF, 0xFFFFFFFF};
        };
    }
}
