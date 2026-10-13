package stone.boot;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class MemoryLoader extends ClassLoader {
    static {
        registerAsParallelCapable();
    }

    private final Map<String, byte[]> entries = new HashMap<>();

    public MemoryLoader(ClassLoader parent, byte[] image) throws IOException {
        super("stone", parent);
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(image))) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null; ) {
                if (!entry.isDirectory()) {
                    entries.put(entry.getName(), zip.readAllBytes());
                }
            }
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        byte[] bytes = entries.get(name.replace('.', '/') + ".class");
        if (bytes == null) {
            return super.loadClass(name, resolve);
        }
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                loaded = defineClass(name, bytes, 0, bytes.length);
            }
            if (resolve) {
                resolveClass(loaded);
            }
            return loaded;
        }
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        byte[] bytes = entries.get(name.startsWith("/") ? name.substring(1) : name);
        return bytes != null ? new ByteArrayInputStream(bytes) : super.getResourceAsStream(name);
    }
}
