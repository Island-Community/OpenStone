package stone.event;

public final class TickEndEvent extends Event {
    public final Phase phase;

    public TickEndEvent(Phase phase) {
        this.phase = phase;
    }
}
