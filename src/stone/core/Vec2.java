package stone.core;

public record Vec2(float x, float y) {
    public Vec2 add(Vec2 o) {
        return new Vec2(x + o.x, y + o.y);
    }
}
