package stone.event;

import net.minecraft.util.IChatComponent;

public final class ChatReceivedEvent extends Event {
    public final byte messageType;
    public final IChatComponent component;

    public ChatReceivedEvent(byte messageType, IChatComponent component) {
        this.messageType = messageType;
        this.component = component;
    }
}
