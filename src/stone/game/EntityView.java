package stone.game;

import stone.core.Vec3;
import net.minecraft.entity.EntityLivingBase;

public final class EntityView {
    public EntityLivingBase handle;
    public int id;
    public boolean isPlayer;
    public boolean invisible;
    public boolean sneaking;
    public boolean onGround;

    public boolean blocking;

    public Vec3 pos = Vec3.ZERO;

    public Vec3 tickPos = Vec3.ZERO;
    public Vec3 eyePos = Vec3.ZERO;
    public float width = 0.6f;
    public float height = 1.8f;
    public float yaw;
    public float pitch;
    public float bodyYaw;

    public float health;
    public float maxHealth;
    public float absorption;
    public int armor;
    public int hurtTime;

    public double distance;

    public Vec3 boxMin() {
        return new Vec3(pos.x() - width * 0.5, pos.y(), pos.z() - width * 0.5);
    }

    public Vec3 boxMax() {
        return new Vec3(pos.x() + width * 0.5, pos.y() + height, pos.z() + width * 0.5);
    }

    public Vec3 tickBoxMin() {
        return new Vec3(tickPos.x() - width * 0.5, tickPos.y(), tickPos.z() - width * 0.5);
    }

    public Vec3 tickBoxMax() {
        return new Vec3(tickPos.x() + width * 0.5, tickPos.y() + height, tickPos.z() + width * 0.5);
    }
}
