package stone.event;

import net.minecraft.world.World;

public final class LoadWorldEvent extends Event {
    public final World world;

    public LoadWorldEvent(World world) {
        this.world = world;
    }
}
