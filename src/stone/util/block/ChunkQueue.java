package stone.util.block;

import java.util.ArrayDeque;
import java.util.Deque;

public final class ChunkQueue {
    private final Deque<ChunkKey> chunkKeys = new ArrayDeque<>();

    public void enqueue(int var1, int var2) {
        this.chunkKeys.addLast(new ChunkKey(var1, var2));
    }

    public ChunkKey poll() {
        return this.chunkKeys.pollFirst();
    }

    public boolean hasPending() {
        return !this.chunkKeys.isEmpty();
    }

    public void clear() {
        this.chunkKeys.clear();
    }

    public int size() {
        return this.chunkKeys.size();
    }
}
