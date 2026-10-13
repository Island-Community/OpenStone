package stone.core;

public final class Time {
    private static final long EPOCH = System.nanoTime();

    private Time() {}

    public static double nowSeconds() {
        return (System.nanoTime() - EPOCH) / 1e9;
    }

    public static long nowMillis() {
        return (System.nanoTime() - EPOCH) / 1_000_000;
    }
}
