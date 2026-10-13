package stone.core;

public final class Timer {
    private long mark;

    public void reset() {
        mark = Time.nowMillis();
    }

    public void elapse(long millis) {
        mark -= millis;
    }

    public long elapsed() {
        return Time.nowMillis() - mark;
    }

    public boolean elapsed(long millis) {
        return elapsed() >= millis;
    }

    public boolean tryReset(long millis) {
        if (!elapsed(millis)) {
            return false;
        }
        reset();
        return true;
    }
}
