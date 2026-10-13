package stone.util.render;

public final class RectUtils {
    private RectUtils() {
    }

    public static float[] expandRect(float var0, float var1, float var2, float var3) {
        return new float[]{var0 - 1.0F, var1 - 1.0F, var2 + 2.0F, var3 + 2.0F};
    }

    public static float[] toScaledPosition(float var0, float var1, float var2, float var3, int var4) {
        return new float[]{var0 * var3, var4 - (var2 + var1) * var3};
    }
}
