package stone.game;

import stone.core.Vec3;
import net.minecraft.client.entity.EntityPlayerSP;

public final class LocalPlayerView {
    public boolean valid;
    public EntityPlayerSP handle;

    public Vec3 pos = Vec3.ZERO;
    public Vec3 eyePos = Vec3.ZERO;
    public Vec3 velocity = Vec3.ZERO;
    public float yaw;
    public float pitch;
    public float lastYaw;
    public float lastPitch;
    public float eyeHeight = 1.62f;

    public float health;
    public float maxHealth = 20.0f;

    public int armor;
    public boolean onGround;
    public boolean sneaking;
    public boolean sprinting;
    public boolean jumping;

    public int hurtTime;
    public int maxHurtTime = 10;
    public double fallDistance;
    public boolean usingItem;

    public int itemUseTicks;
    public int age;
}
