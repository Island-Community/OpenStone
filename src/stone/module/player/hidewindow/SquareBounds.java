package stone.module.player.hidewindow;

public final class SquareBounds {
    private SquareBounds() {
    }

    public static int Yvhab2(int var0, double var1) {
        return Math.round(var0 * (float)var1);
    }

    public static float[] ZlAtiX(float var0, float var1, int var2, float var3) {
        float var4 = var2 / 2.0F;
        return new float[]{var0 - var4 - var3, var1 - var4 - var3, var0 + var4 + var3, var1 + var4 + var3};
    }
}
