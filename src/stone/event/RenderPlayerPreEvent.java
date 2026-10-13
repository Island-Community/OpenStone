package stone.event;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.player.EntityPlayer;

public final class RenderPlayerPreEvent extends RenderPlayerEvent {
    public RenderPlayerPreEvent(EntityPlayer entityPlayer, double x, double y, double z, float partialTicks,
                                ModelBiped modelBiped) {
        super(entityPlayer, x, y, z, partialTicks, modelBiped);
    }
}
