package stone.util.render;

public final class FakePlayerRenderer$3 {
    private final long uzX;
    private final FakePlayerRenderer$2 renderState;

    FakePlayerRenderer$3(long var1, FakePlayerRenderer$2 var3) {
        this.uzX = var1;
        this.renderState = var3;
    }

    public static long getSnapshotTimeMillis(FakePlayerRenderer$3 var0) {
        return var0.uzX;
    }

    public static FakePlayerRenderer$2 getRenderState(FakePlayerRenderer$3 var0) {
        return var0.renderState;
    }
}
