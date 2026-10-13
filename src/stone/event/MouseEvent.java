package stone.event;

public final class MouseEvent extends Event {
    public final int button;
    public final boolean pressed;
    public final int mouseX;
    public final int mouseY;
    public final int deltaX;
    public final int deltaY;
    public final int wheelDelta;

    public MouseEvent(int button, boolean pressed, int mouseX, int mouseY, int deltaX, int deltaY, int wheelDelta) {
        this.button = button;
        this.pressed = pressed;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.wheelDelta = wheelDelta;
    }
}
