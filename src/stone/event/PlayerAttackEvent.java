package stone.event;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public final class PlayerAttackEvent extends Event {
    public final EntityPlayer entityPlayer;
    public final Entity entity;

    public PlayerAttackEvent(EntityPlayer entityPlayer, Entity entity) {
        this.entityPlayer = entityPlayer;
        this.entity = entity;
    }
}
