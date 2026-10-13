package stone.event;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.player.EntityPlayer;

public abstract class RenderPlayerEvent extends Event {
    public final EntityPlayer entityPlayer;
    public final double x;
    public final double y;
    public final double z;
    public final float partialTicks;
    public final ModelBiped modelBiped;

    protected RenderPlayerEvent(EntityPlayer entityPlayer, double x, double y, double z, float partialTicks,
                                ModelBiped modelBiped) {
        this.entityPlayer = entityPlayer;
        this.x = x;
        this.y = y;
        this.z = z;
        this.partialTicks = partialTicks;
        this.modelBiped = modelBiped;
    }
}
