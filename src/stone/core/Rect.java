package stone.core;

public record Rect(float x, float y, float width, float height) {
    public static final Rect EMPTY = new Rect(0, 0, 0, 0);

    public float right() {
        return x + width;
    }

    public float bottom() {
        return y + height;
    }

    public float centerX() {
        return x + width * 0.5f;
    }

    public float centerY() {
        return y + height * 0.5f;
    }

    public boolean contains(float px, float py) {
        return px >= x && py >= y && px < x + width && py < y + height;
    }

    public Rect inset(float amount) {
        return new Rect(x + amount, y + amount, width - amount * 2.0f, height - amount * 2.0f);
    }
}
