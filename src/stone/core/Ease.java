package stone.core;

public final class Ease {
    private Ease() {}

    public static float outCubic(float t) {
        float inverted = 1.0f - t;
        return 1.0f - inverted * inverted * inverted;
    }

    public static float outQuint(float t) {
        float inverted = 1.0f - t;
        return 1.0f - inverted * inverted * inverted * inverted * inverted;
    }

    public static float outExpo(float t) {
        return t >= 1.0f ? 1.0f : 1.0f - (float) Math.pow(2.0, -10.0 * t);
    }

    public static float outBack(float t, float overshoot) {
        float c = overshoot + 1.0f;
        float inverted = t - 1.0f;
        return 1.0f + c * inverted * inverted * inverted + overshoot * inverted * inverted;
    }

    public static float springIn(float t) {
        if (t <= 0.0f) {
            return 0.0f;
        }
        if (t >= 1.0f) {
            return 1.0f;
        }
        float frequency = 9.4f;
        float damping = 0.62f;
        float ringing = frequency * (float) Math.sqrt(1.0f - damping * damping);
        float decay = (float) Math.exp(-damping * frequency * t);
        return 1.0f - decay * ((float) Math.cos(ringing * t)
                + (damping * frequency / ringing) * (float) Math.sin(ringing * t));
    }

    public static float morph(float t) {
        t = Maths.clamp(t, 0.0f, 1.0f);
        float smoother = t * t * t * (t * (t * 6.0f - 15.0f) + 10.0f);
        return 1.0f - (float) Math.pow(1.0f - smoother, 1.30f);
    }

    public static float window(float clock, float from, float to) {
        return Maths.clamp((clock - from) / Math.max(0.0001f, to - from), 0.0f, 1.0f);
    }
}
