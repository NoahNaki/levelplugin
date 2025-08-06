package me.nakilex.levelplugin.screenmenu;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Simple registry for sections. */
public class SectionManager {
    private final Map<String, Section> sections = new HashMap<>();

    public void add(String key, Section section) {
        sections.put(key, section);
    }

    public Section get(String key) {
        return sections.get(key);
    }

    public boolean has(String key) {
        return sections.containsKey(key);
    }

    public Set<String> keySet() {
        return sections.keySet();
    }

    public MenuLayout getLayout(String fullKey) {
        if (fullKey == null || !fullKey.contains(":")) return null;
        String[] parts = fullKey.split(":", 2);
        Section sec = sections.get(parts[0]);
        return sec == null ? null : sec.layouts.get(parts[1]);
    }

    public Map<String, Section> all() {
        return sections;
    }

    public void clear() {
        sections.clear();
    }
}
