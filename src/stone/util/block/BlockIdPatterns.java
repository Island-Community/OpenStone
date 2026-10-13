package stone.util.block;

public final class BlockIdPatterns {
    private BlockIdPatterns() {
    }

    public static boolean hasWildcardSuffix(String var0) {
        return var0 != null && var0.endsWith(":*");
    }

    public static String stripWildcardSuffix(String var0) {
        return hasWildcardSuffix(var0) ? var0.substring(0, var0.length() - 2) : normalizeBlockId(var0);
    }

    public static String normalizeBlockId(String var0) {
        if (var0 != null && !var0.isEmpty()) {
            String[] var1 = var0.split(":");
            if (var1.length < 2) {
                return null;
            } else {
                return var1.length == 2 ? var0 : var1[0] + ':' + var1[1];
            }
        } else {
            return null;
        }
    }
}
