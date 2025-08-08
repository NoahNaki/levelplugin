package me.nakilex.levelplugin.cursormenu.layout;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple registry for available menu sections. Sections are addressed by a
 * case-insensitive key.
 */
public class SectionManager {
    private final Map<String, Section> sections = new ConcurrentHashMap<>();

    public void addSection(Section section) {
        sections.put(section.getKey().toLowerCase(), section);
    }

    public Section get(String key) {
        if (key == null) return null;
        return sections.get(key.toLowerCase());
    }

    public Collection<Section> getAll() {
        return sections.values();
    }

    public void clear() {
        sections.clear();
    }
}
