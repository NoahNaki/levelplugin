package me.nakilex.levelplugin.cursormenu.menu;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages registered sections and layout lookup.
 */
public class SectionManager {
    private final Map<String, Section> sections = new ConcurrentHashMap<>();

    public void addSection(String key, Section section) {
        sections.put(key, section);
    }

    public Section get(String key) {
        return sections.get(key);
    }

    public MenuLayout getLayout(String sectionKey, int index) {
        Section s = sections.get(sectionKey);
        if (s == null || index < 0 || index >= s.getLayouts().size()) return null;
        return s.getLayouts().get(index);
    }

    public Collection<Section> getAll() {
        return sections.values();
    }

    public Set<String> keySet() { return sections.keySet(); }

    public void clear() { sections.clear(); }
}
