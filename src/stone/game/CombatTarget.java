package stone.game;

public final class CombatTarget {
    private static final double MAX_AGE_SECONDS = 0.35;

    private static int id = -1;
    private static double at;

    private CombatTarget() {}

    public static void publish(int entityId, double atSeconds) {
        id = entityId;
        at = atSeconds;
    }

    public static void clear() {
        id = -1;
    }

    public static int current(double nowSeconds) {
        return id < 0 || nowSeconds - at > MAX_AGE_SECONDS ? -1 : id;
    }
}
