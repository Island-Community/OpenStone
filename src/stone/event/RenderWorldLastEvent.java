package stone.event;

public final class RenderWorldLastEvent extends Event {
    public final float partialTicks;

    public RenderWorldLastEvent(float partialTicks) {
        this.partialTicks = partialTicks;
    }
}
