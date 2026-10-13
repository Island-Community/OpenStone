package stone.event;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public final class EntityJoinWorldEvent extends Event {
    public final Entity entity;
    public final World world;

    public EntityJoinWorldEvent(Entity entity, World world) {
        this.entity = entity;
        this.world = world;
    }
}
