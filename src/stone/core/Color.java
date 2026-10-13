package stone.core;

public record Color(float r, float g, float b, float a) {
    public static final Color WHITE = new Color(1, 1, 1, 1);

    public static Color rgba(int value) {
        return new Color(((value >>> 24) & 0xFF) / 255.0f, ((value >>> 16) & 0xFF) / 255.0f,
                ((value >>> 8) & 0xFF) / 255.0f, (value & 0xFF) / 255.0f);
    }

    public static Color rgb(int value) {
        return rgb(value, 1.0f);
    }

    public static Color rgb(int value, float alpha) {
        return new Color(((value >>> 16) & 0xFF) / 255.0f, ((value >>> 8) & 0xFF) / 255.0f,
                (value & 0xFF) / 255.0f, alpha);
    }

    public Color withAlpha(float alpha) {
        return new Color(r, g, b, a * alpha);
    }

    private static int toByte(float v) {
        return (int) (Maths.clamp(v, 0.0f, 1.0f) * 255.0f + 0.5f);
    }

    public int packed() {
        return (toByte(r) << 24) | (toByte(g) << 16) | (toByte(b) << 8) | toByte(a);
    }

    public int argb() {
        return (toByte(a) << 24) | (toByte(r) << 16) | (toByte(g) << 8) | toByte(b);
    }

    public static Color mix(Color from, Color to, float t) {
        return new Color(Maths.lerp(from.r, to.r, t), Maths.lerp(from.g, to.g, t),
                Maths.lerp(from.b, to.b, t), Maths.lerp(from.a, to.a, t));
    }

    public static Color hsv(float hue, float saturation, float value) {
        return hsv(hue, saturation, value, 1.0f);
    }

    public static Color hsv(float hue, float saturation, float value, float alpha) {
        hue = hue - (float) Math.floor(hue);
        float h = hue * 6.0f;
        int sector = (int) h;
        float f = h - sector;
        float p = value * (1.0f - saturation);
        float q = value * (1.0f - saturation * f);
        float t = value * (1.0f - saturation * (1.0f - f));
        return switch (sector % 6) {
            case 0 -> new Color(value, t, p, alpha);
            case 1 -> new Color(q, value, p, alpha);
            case 2 -> new Color(p, value, t, alpha);
            case 3 -> new Color(p, q, value, alpha);
            case 4 -> new Color(t, p, value, alpha);
            default -> new Color(value, p, q, alpha);
        };
    }
}
