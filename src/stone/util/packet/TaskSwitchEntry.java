package stone.util.packet;

public final class TaskSwitchEntry extends PacketQueueEntry {
    private final PacketListenerRegistration task;

    public TaskSwitchEntry(PacketListenerRegistration var1) {
        this.task = var1;
    }

    @Override
    public PacketListenerRegistration nok9(PacketListenerRegistration var1, HandledPacketTracker var2) {
        return this.task;
    }
}
