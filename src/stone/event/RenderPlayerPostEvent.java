package stone.event;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.player.EntityPlayer;

public final class RenderPlayerPostEvent extends RenderPlayerEvent {
    public RenderPlayerPostEvent(EntityPlayer entityPlayer, double x, double y, double z, float partialTicks,
                                 ModelBiped modelBiped) {
        super(entityPlayer, x, y, z, partialTicks, modelBiped);
    }
}
