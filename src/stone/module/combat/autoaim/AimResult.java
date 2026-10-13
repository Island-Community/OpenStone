package stone.module.combat.autoaim;

import net.minecraft.util.Vec3;

public final class AimResult {
    private final AimResult$0 xLca;
    private final float psT;
    private final float PFqVa;
    private final int OBNIo;
    private final TargetPrediction prediction;
    private final Vec3 vec3;

    public AimResult(AimResult$0 var1, float var2, float var3, int var4, TargetPrediction var7, Vec3 var9) {
        this.xLca = var1;
        this.psT = var2;
        this.PFqVa = var3;
        this.OBNIo = var4;
        this.prediction = var7;
        this.vec3 = var9;
    }

    public AimResult$0 WYqjD() {
        return this.xLca;
    }

    public float getYaw() {
        return this.psT;
    }

    public float getPitch() {
        return this.PFqVa;
    }

    public int getTicks() {
        return this.OBNIo;
    }

    public TargetPrediction getPrediction() {
        return this.prediction;
    }

    public Vec3 kahN() {
        return this.vec3;
    }
}
