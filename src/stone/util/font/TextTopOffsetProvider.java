package stone.util.font;

public interface TextTopOffsetProvider {
    default int getTextTopOffset() {
        return ComponentTextMetrics.top();
    }
}
