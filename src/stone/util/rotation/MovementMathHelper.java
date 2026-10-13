package stone.util.rotation;

public final class MovementMathHelper {
    private MovementMathHelper() {
    }

    public static float computeMoveDirectionRadians(float var0, float var1, float var2) {
        float var3 = var1 < 0.0F ? -0.5F : (var1 > 0.0F ? 0.5F : 1.0F);
        float var4 = var1 < 0.0F ? var0 + 180.0F : var0;
        if (var2 != 0.0F) {
            if (var2 > 0.0F) {
                var4 -= 90.0F * var3;
            } else if (var2 < 0.0F) {
                var4 += 90.0F * var3;
            }
        }

        return var4 * (float) (Math.PI / 180.0);
    }
}
