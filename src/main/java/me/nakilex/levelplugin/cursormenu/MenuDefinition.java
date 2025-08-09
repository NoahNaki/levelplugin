package me.nakilex.levelplugin.cursormenu;

import java.util.Collections;
import java.util.List;

/**
 * Simple data holder for a parsed menu. Elements and actions can be
 * expanded in the future as more features are implemented.
 */
public class MenuDefinition {
    private final String id;
    private final String title;
    private final List<String> elements; // placeholder for future elements

    public MenuDefinition(String id, String title, List<String> elements) {
        this.id = id;
        this.title = title;
        this.elements = elements == null ? Collections.emptyList() : elements;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getElements() {
        return elements;
    }
}
