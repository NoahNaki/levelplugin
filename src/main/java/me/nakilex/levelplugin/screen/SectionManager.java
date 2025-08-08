package me.nakilex.levelplugin.screen;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Simple registry for {@link Section} objects.
 */
public class SectionManager {

    private final Map<String, Section> sections = new HashMap<>();

    public void addSection(String key, Section section) {
        sections.put(key.toLowerCase(), section);
    }

    public Section get(String key) {
        return sections.get(key.toLowerCase());
    }

    public MenuLayout getLayout(String key, int index) {
        Section s = get(key);
        if (s == null) return null;
        if (index < 0 || index >= s.getLayouts().size()) return null;
        return s.getLayouts().get(index);
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
