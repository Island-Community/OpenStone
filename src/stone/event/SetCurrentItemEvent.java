package stone.event;

public final class SetCurrentItemEvent extends Event {
    public int slot;

    public SetCurrentItemEvent(int slot) {
        this.slot = slot;
    }
}
