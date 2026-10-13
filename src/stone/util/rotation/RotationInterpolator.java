package stone.util.rotation;

public final class RotationInterpolator {
    private RotationInterpolator() {
    }

    public static float tfpXj(float var0, float var1) {
        float var2 = ((var0 - var1 + 180.0F) % 360.0F + 360.0F) % 360.0F - 180.0F;
        return var1 + var2;
    }
}
