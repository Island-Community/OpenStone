package stone.module.combat.autoaim;

import net.minecraft.util.Vec3;

public final class AimSolver$1 {
    public final float yaw;
    public final float SLrgWv;
    public final int travelTicks;
    public final double exactTravelTicks;
    public final double distanceError;
    public final double distanceDelta;
    public boolean hitsTarget;
    public TargetPrediction predictedPosition;
    public Vec3 vec3;

    public AimSolver$1(float var1, float var2, double var3, double var5) {
        this.yaw = var1;
        this.SLrgWv = var2;
        this.exactTravelTicks = var3;
        this.travelTicks = (int)Math.ceil(var3);
        this.distanceDelta = var5;
        this.distanceError = Math.abs(var5);
    }
}
