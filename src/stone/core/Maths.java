package stone.core;

public final class Maths {
    public static final float PI = (float) Math.PI;
    public static final float DEG_TO_RAD = PI / 180.0f;

    private Maths() {}

    public static float clamp(float value, float low, float high) {
        return value < low ? low : (value > high ? high : value);
    }

    public static double clamp(double value, double low, double high) {
        return value < low ? low : (value > high ? high : value);
    }

    public static int clamp(int value, int low, int high) {
        return value < low ? low : (value > high ? high : value);
    }

    public static int floor(double value) {
        return (int) Math.floor(value);
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static float approach(float current, float target, float rate, float deltaTime) {
        float step = 1.0f - (float) Math.exp(-rate * deltaTime);
        float next = current + (target - current) * step;
        return Math.abs(target - next) < 0.0015f ? target : next;
    }

    public static float wrapDegrees(float degrees) {
        degrees %= 360.0f;
        if (degrees >= 180.0f) {
            degrees -= 360.0f;
        }
        if (degrees < -180.0f) {
            degrees += 360.0f;
        }
        return degrees;
    }

    public static float angleDifference(float from, float to) {
        return wrapDegrees(to - from);
    }
}
