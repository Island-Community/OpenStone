package stone.module.combat.autoaim;

public final class TargetPrediction {
    private final double YQdwM9;
    private final double y;
    private final double z;
    private final double lNl;

    public TargetPrediction(double var1, double var3, double var5, double var9) {
        this.YQdwM9 = var1;
        this.y = var3;
        this.z = var5;
        this.lNl = var9;
    }

    public double RAYFZ() {
        return this.YQdwM9;
    }

    public double fmmQ() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public double getConfidence() {
        return this.lNl;
    }
}
