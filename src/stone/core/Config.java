package stone.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class Config {

    private final Map<String, Map<String, String>> sections = new TreeMap<>();

    public boolean load(Path path) {
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.debug("config", "no config on disk yet, starting from defaults");
            return false;
        }
        sections.clear();
        String section = "";
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.charAt(0) == '#' || line.charAt(0) == ';') {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1);
                sections.computeIfAbsent(section, name -> new TreeMap<>());
                continue;
            }
            int separator = line.indexOf('=');
            if (separator < 0 || section.isEmpty()) {
                continue;
            }
            sections.get(section).put(line.substring(0, separator).strip(), line.substring(separator + 1).strip());
        }
        Log.info("config", "loaded %d sections", sections.size());
        return true;
    }

    public boolean save(Path path) {
        StringBuilder content = new StringBuilder();
        sections.forEach((section, entries) -> {
            content.append("\n[").append(section).append("]\n");
            entries.forEach((key, value) -> content.append(key).append(" = ").append(value).append('\n'));
        });
        try {
            Files.writeString(path, content, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            Log.error("config", "config write failed: %s", e);
            return false;
        }
    }

    public void set(String section, String key, String value) {
        sections.computeIfAbsent(section, name -> new TreeMap<>()).put(key, value);
    }

    public void setBool(String section, String key, boolean value) {
        set(section, key, value ? "true" : "false");
    }

    public void setNumber(String section, String key, float value, int decimals) {
        set(section, key, String.format(Locale.ROOT, "%." + Math.max(decimals, 0) + "f", value));
    }

    public void setInt(String section, String key, int value) {
        set(section, key, Integer.toString(value));
    }

    public String get(String section, String key, String fallback) {
        Map<String, String> entries = sections.get(section);
        return entries == null ? fallback : entries.getOrDefault(key, fallback);
    }

    public boolean getBool(String section, String key, boolean fallback) {
        String value = get(section, key, fallback ? "true" : "false");
        return value.equals("true") || value.equals("1") || value.equals("yes");
    }

    public float getNumber(String section, String key, float fallback) {
        try {
            return Float.parseFloat(get(section, key, ""));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public int getInt(String section, String key, int fallback) {
        try {
            return Integer.parseInt(get(section, key, ""));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public boolean hasSection(String section) {
        return sections.containsKey(section);
    }

    public void clearSection(String section) {
        sections.remove(section);
    }

    public List<String> keys(String section) {
        Map<String, String> entries = sections.get(section);
        return entries == null ? List.of() : new ArrayList<>(entries.keySet());
    }

    public void clear() {
        sections.clear();
    }
}
