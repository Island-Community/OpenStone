package stone.event;

public final class UpdateWalkingPlayerEvent extends Event {
    private float yaw;
    private float pitch;
    private boolean sneaking;

    public UpdateWalkingPlayerEvent(float yaw, float pitch, boolean sneaking) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.sneaking = sneaking;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float value) {
        yaw = value;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float value) {
        pitch = value;
    }

    public boolean isSneaking() {
        return sneaking;
    }

    public void setSneaking(boolean value) {
        sneaking = value;
    }
}
