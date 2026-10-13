package stone.event;

public final class RotationEvent extends Event {
    public static final int PRIORITY_NONE = 0;
    public static final int PRIORITY_MINIMAL = 10;
    public static final int PRIORITY_LOW = 20;
    public static final int PRIORITY_MEDIUM = 30;
    public static final int PRIORITY_ELEVATED = 40;
    public static final int PRIORITY_HIGH = 45;
    public static final int PRIORITY_URGENT = 50;
    public static final int PRIORITY_CRITICAL = 55;
    public static final int PRIORITY_MAXIMUM = 60;

    public Float yaw;
    public Float pitch;
    public boolean suppressRotations;
    private int appliedPriority = Integer.MIN_VALUE;

    public RotationEvent(Float yaw, Float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void setYaw(Float value) {
        setYaw(value, PRIORITY_NONE);
    }

    public void setPitch(Float value) {
        setPitch(value, PRIORITY_NONE);
    }

    public boolean setYaw(Float value, int priority) {
        if (priority < appliedPriority) {
            return false;
        }
        yaw = value;
        appliedPriority = priority;
        return true;
    }

    public boolean setPitch(Float value, int priority) {
        if (priority < appliedPriority) {
            return false;
        }
        pitch = value;
        appliedPriority = priority;
        return true;
    }

    public boolean setRotation(Float yawValue, Float pitchValue, int priority) {
        if (priority < appliedPriority) {
            return false;
        }
        yaw = yawValue;
        pitch = pitchValue;
        appliedPriority = priority;
        return true;
    }

    public int appliedPriority() {
        return appliedPriority;
    }
}
