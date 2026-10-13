package stone.module.combat.autoblock;

public final class IncomingHitPredictor$1 {
    private final double approachDistance;
    private final double closingDistancePerTick;
    private final boolean targetMovingAway;

    IncomingHitPredictor$1(double var1, double var3, boolean var5) {
        this.approachDistance = var1;
        this.closingDistancePerTick = var3;
        this.targetMovingAway = var5;
    }

    public static double getApproachDistance(IncomingHitPredictor$1 var0) {
        return var0.approachDistance;
    }

    public static double VGdeC(IncomingHitPredictor$1 var0) {
        return var0.closingDistancePerTick;
    }

    public static boolean isTargetMovingAway(IncomingHitPredictor$1 var0) {
        return var0.targetMovingAway;
    }
}
