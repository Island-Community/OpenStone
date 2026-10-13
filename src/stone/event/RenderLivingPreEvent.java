package stone.event;

import net.minecraft.entity.EntityLivingBase;

public final class RenderLivingPreEvent extends RenderLivingEvent {
    public RenderLivingPreEvent(EntityLivingBase entityLivingBase, double x, double y, double z, float partialTicks) {
        super(entityLivingBase, x, y, z, partialTicks);
    }
}
