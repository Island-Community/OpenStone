package stone.event;

public abstract class Event {
    private boolean cancelled;

    public boolean cancelled() {
        return cancelled;
    }

    public void setCancelled(boolean value) {
        cancelled = value;
    }
}
