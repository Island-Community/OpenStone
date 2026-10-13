package stone.util.font;

public interface LineHeightFont extends FontHeightProvider {
    default int getLineHeight() {
        return ComponentTextMetrics.height(this);
    }
}
