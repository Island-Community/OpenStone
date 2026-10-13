package stone.module.combat.autoblock;

public final class IncomingHitPredictor$2 {
    private final long baY6;
    private boolean VgW;

    IncomingHitPredictor$2(long var1) {
        this.baY6 = var1;
    }

    public static boolean isInvalidated(IncomingHitPredictor$2 var0) {
        return var0.VgW;
    }

    public static boolean PITAi(IncomingHitPredictor$2 var0, boolean var1) {
        return var0.VgW = var1;
    }

    public static long getRecordedTime(IncomingHitPredictor$2 var0) {
        return var0.baY6;
    }
}
