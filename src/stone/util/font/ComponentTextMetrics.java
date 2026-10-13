package stone.util.font;

public final class ComponentTextMetrics {
    private ComponentTextMetrics() {
    }

    public static int height(FontHeightProvider var0) {
        return var0.getFontHeight();
    }

    public static int top() {
        return 0;
    }
}
