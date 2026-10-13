package stone.event;

public final class MoveFlyingEvent extends Event {
    private float strafe;
    private float forward;
    private float friction;
    private float yaw;

    public MoveFlyingEvent(float strafe, float forward, float friction, float yaw) {
        this.strafe = strafe;
        this.forward = forward;
        this.friction = friction;
        this.yaw = yaw;
    }

    public float getStrafe() {
        return strafe;
    }

    public void setStrafe(float value) {
        strafe = value;
    }

    public float getForward() {
        return forward;
    }

    public void setForward(float value) {
        forward = value;
    }

    public float getFriction() {
        return friction;
    }

    public void setFriction(float value) {
        friction = value;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float value) {
        yaw = value;
    }
}
