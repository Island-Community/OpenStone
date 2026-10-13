package stone.util;

import java.util.concurrent.atomic.AtomicBoolean;

public final class OneWayFlag {
    private final AtomicBoolean puT = new AtomicBoolean();

    public boolean isSet() {
        return this.puT.get();
    }

    public void DzqT() {
        this.puT.set(true);
    }
}
