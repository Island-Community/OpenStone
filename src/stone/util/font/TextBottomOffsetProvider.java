package stone.util.font;

public interface TextBottomOffsetProvider extends FontHeightProvider {
    default int getTextBottomOffset() {
        return ComponentTextMetrics.height(this);
    }
}
