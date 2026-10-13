package stone.game;

public final class RotationTarget {
    public float yaw;
    public float pitch;

    public float speed = 180.0f;

    public float smoothing = 0.3f;

    public int jitter;

    public boolean silent;
    public int priority;

    public RotationTarget() {}

    public RotationTarget(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }
}
