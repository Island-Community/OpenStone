package stone.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public final class Assets {
    private Assets() {}

    public static InputStream open(String path) {
        return Assets.class.getClassLoader().getResourceAsStream(path.startsWith("/") ? path.substring(1) : path);
    }

    public static byte[] read(String path) {
        try (InputStream stream = Assets.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("asset missing from the image: " + path);
            }
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("reading asset " + path, e);
        }
    }
}
