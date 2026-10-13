package stone.event;

public final class ChangeCurrentItemEvent extends Event {
    public final int scrollDirection;
    public final int currentItem;

    public ChangeCurrentItemEvent(int scrollDirection, int currentItem) {
        this.scrollDirection = scrollDirection;
        this.currentItem = currentItem;
    }
}
