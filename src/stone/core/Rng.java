package stone.core;

public final class Rng {
    private int state;

    public Rng() {
        this(0x1F123BB5);
    }

    public Rng(int seed) {
        state = seed == 0 ? 1 : seed;
    }

    public int next() {
        state ^= state << 13;
        state ^= state >>> 17;
        state ^= state << 5;
        return state;
    }

    public float unit() {
        return (next() >>> 8) / 16777216.0f;
    }

    public float range(float low, float high) {
        return low + unit() * (high - low);
    }

    public int range(int low, int high) {
        if (high <= low) {
            return low;
        }
        return low + (int) Integer.remainderUnsigned(next(), high - low + 1);
    }
}
