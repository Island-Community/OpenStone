package stone.event;

import net.minecraft.entity.EntityLivingBase;

public final class JumpEvent extends Event {
    private final EntityLivingBase entity;
    private float motionY;
    private float yaw;
    private boolean sprinting;

    public JumpEvent(EntityLivingBase entity, float motionY, float yaw, boolean sprinting) {
        this.entity = entity;
        this.motionY = motionY;
        this.yaw = yaw;
        this.sprinting = sprinting;
    }

    public EntityLivingBase getEntityLiving() {
        return entity;
    }

    public float getMotionY() {
        return motionY;
    }

    public void setMotionY(float value) {
        motionY = value;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float value) {
        yaw = value;
    }

    public boolean isSprinting() {
        return sprinting;
    }

    public void setSprinting(boolean value) {
        sprinting = value;
    }
}
