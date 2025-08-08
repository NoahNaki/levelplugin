package me.nakilex.levelplugin.screen.menu;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores and retrieves menu sections. A simple concurrent map allows the
 * manager to be used safely from async tasks if desired.
 */
public class SectionManager {
    private final Map<String, Section> sections = new ConcurrentHashMap<>();

    public void addSection(Section section) {
        sections.put(section.getKey().toLowerCase(), section);
    }

    public Section get(String key) {
        return sections.get(key.toLowerCase());
    }

    public MenuLayout getLayout(String path) {
        String[] parts = path.split(":", 2);
        if (parts.length != 2) return null;
        Section sec = get(parts[0]);
        if (sec == null) return null;
        try {
            int index = Integer.parseInt(parts[1]);
            if (index < 0 || index >= sec.getLayouts().size()) return null;
            return sec.getLayouts().get(index);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public Collection<Section> getAll() {
        return Collections.unmodifiableCollection(sections.values());
    }

    public void clear() {
        sections.clear();
    }

    public Iterable<String> keySet() {
        return sections.keySet();
    }
}
