package stone.event;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public final class AttackEntityEvent extends Event {
    public final Entity entity;
    public final EntityPlayer entityPlayer;
    public final boolean isPreAttack;

    public AttackEntityEvent(Entity entity, EntityPlayer entityPlayer, boolean isPreAttack) {
        this.entity = entity;
        this.entityPlayer = entityPlayer;
        this.isPreAttack = isPreAttack;
    }
}
