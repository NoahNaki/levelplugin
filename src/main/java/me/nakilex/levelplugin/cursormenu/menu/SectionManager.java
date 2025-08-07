package me.nakilex.levelplugin.cursormenu.menu;

import java.util.*;

/**
 * Stores all registered sections. Keys are case-insensitive for
 * convenience.
 */
public class SectionManager {
    private final Map<String, Section> sections = new HashMap<>();

    public void addSection(String key, Section section) {
        sections.put(key.toLowerCase(Locale.ROOT), section);
    }

    public Section get(String key) {
        return sections.get(key.toLowerCase(Locale.ROOT));
    }

    public Collection<Section> getAll() {
        return sections.values();
    }

    public Set<String> keySet() {
        return sections.keySet();
    }

    public void clear() {
        sections.clear();
    }
}
