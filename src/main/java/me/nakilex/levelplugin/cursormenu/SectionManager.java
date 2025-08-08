package me.nakilex.levelplugin.cursormenu;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores all configured {@link Section} objects. The manager offers convenient
 * lookups for sections and their respective {@link MenuLayout} entries.
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
        Section section = sections.get(sectionKey);
        if (section == null) {
            return null;
        }
        if (index < 0 || index >= section.getLayouts().size()) {
            return null;
        }
        return section.getLayouts().get(index);
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
