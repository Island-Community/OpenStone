package stone.module.movement;

import stone.event.LoadWorldEvent;
import stone.event.MoveInputEvent;
import stone.event.Subscribe;

public final class StasisInputLock {
    private static final int LOCK_TICKS = 2;
    private int lockedTicks;

    StasisInputLock() {
    }

    private void lock() {
        this.lockedTicks = LOCK_TICKS;
    }

    private void clear() {
        this.lockedTicks = 0;
    }

    @Subscribe
    public void onMoveInput(MoveInputEvent event) {
        if (this.lockedTicks > 0) {
            event.setMoveForward(0.0F);
            event.setMoveStrafe(0.0F);
            event.setJumping(false);
            event.setSneaking(false);
            this.lockedTicks--;
        }
    }

    @Subscribe
    public void onLoadWorld(LoadWorldEvent event) {
        this.clear();
    }

    public static void reset(StasisInputLock lock) {
        lock.clear();
    }

    public static void lockInput(StasisInputLock lock) {
        lock.lock();
    }
}
