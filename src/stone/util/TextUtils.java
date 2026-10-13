package stone.util;

import java.util.Random;

public final class TextUtils {

    private TextUtils() {
    }

    public static boolean isWholeNumber(double var0) {
        return var0 == Math.floor(var0);
    }

    public static String ASXEvc(double var0) {
        return isWholeNumber(var0) ? Integer.toString((int)var0) : Double.toString(var0);
    }

    public static int randomIntInclusive(Random var0, int var1, int var2) {
        return var1 + var0.nextInt(var2 - var1 + 1);
    }

    public static double randomDoubleInRange(Random var0, double var1, double var3) {
        return var1 + var0.nextDouble() * (var3 - var1);
    }

    public static String QJdK(String var0) {
        return var0 == null ? "" : var0.replace('&', '§');
    }

    public static String NTfs(String var0) {
        if (var0 == null) {
            return "";
        } else {
            for (int var1 = 0; var1 + 1 < var0.length(); var1++) {
                if (var0.charAt(var1) == 167) {
                    char var2 = var0.charAt(var1 + 1);
                    if ("0123456789abcdef".indexOf(Character.toLowerCase(var2)) >= 0) {
                        return new String(new char[]{'§', var2});
                    }
                }
            }

            return "";
        }
    }

    public static int withAlpha(int var0, int var1) {
        return var0 & 16777215 | var1 << 24;
    }
}
