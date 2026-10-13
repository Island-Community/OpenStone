package stone.module.render.freelook;

public final class FreeLookMath {
    private FreeLookMath() {
    }

    public static int clampPerspective(int var0) {
        return var0 < 0 ? 0 : Math.min(var0, 2);
    }

    public static boolean needsFovRestore(boolean var0, float var1, float var2, boolean var3) {
        return var0 || var1 == var2 || var3;
    }
}
