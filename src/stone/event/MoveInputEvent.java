package stone.event;

public final class MoveInputEvent extends Event {
    private float moveForward;
    private float moveStrafe;
    private boolean jumping;
    private boolean sneaking;

    public MoveInputEvent(float moveForward, float moveStrafe, boolean jumping, boolean sneaking) {
        this.moveForward = moveForward;
        this.moveStrafe = moveStrafe;
        this.jumping = jumping;
        this.sneaking = sneaking;
    }

    public float getMoveForward() {
        return moveForward;
    }

    public void setMoveForward(float value) {
        moveForward = value;
    }

    public float getMoveStrafe() {
        return moveStrafe;
    }

    public void setMoveStrafe(float value) {
        moveStrafe = value;
    }

    public boolean isJumping() {
        return jumping;
    }

    public void setJumping(boolean value) {
        jumping = value;
    }

    public boolean isSneaking() {
        return sneaking;
    }

    public void setSneaking(boolean value) {
        sneaking = value;
    }
}
