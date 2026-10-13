package stone.util;

import java.util.function.IntPredicate;

public final class KeyCodes {
    private KeyCodes() {
    }

    public static boolean isDown(int var0, IntPredicate var1, IntPredicate var2) {
        return var0 >= 0 ? var1.test(var0) : var2.test(var0 + 100);
    }
}
