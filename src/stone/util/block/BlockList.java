package stone.util.block;

import java.util.ArrayList;
import java.util.List;

public class BlockList {
    private final List<String> entries = new ArrayList<>();

    public void addEntry(String id) {
        if (!entries.contains(id)) {
            entries.add(id);
        }
    }

    public List<String> getEntries() {
        return entries;
    }

    public boolean containsEntry(String id) {
        if (entries.contains(id)) {
            return true;
        }
        String base = baseId(id);
        return base != null && entries.contains(base + ":*");
    }

    private static String baseId(String id) {
        if (id.endsWith(":*")) {
            return id.substring(0, id.length() - 2);
        }
        int end = id.length();
        while (end > 0 && id.charAt(end - 1) == ':') {
            end--;
        }
        int first = id.indexOf(':');
        if (first < 0 || first >= end) {
            return null;
        }
        int second = id.indexOf(':', first + 1);
        return second >= 0 && second < end ? id.substring(0, second) : id;
    }
}
