package stone.event;

import net.minecraft.entity.EntityLivingBase;

public abstract class RenderLivingEvent extends Event {
    public final EntityLivingBase entityLivingBase;
    public final double x;
    public final double y;
    public final double z;
    public final float partialTicks;

    protected RenderLivingEvent(EntityLivingBase entityLivingBase, double x, double y, double z, float partialTicks) {
        this.entityLivingBase = entityLivingBase;
        this.x = x;
        this.y = y;
        this.z = z;
        this.partialTicks = partialTicks;
    }
}
