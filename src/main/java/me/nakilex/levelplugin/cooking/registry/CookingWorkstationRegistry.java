package me.nakilex.levelplugin.cooking.registry;

import me.nakilex.levelplugin.cooking.model.CookingWorkstationType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory registry for config-backed cooking workstation type definitions. */
public class CookingWorkstationRegistry {
    private final Map<String, CookingWorkstationType> workstations = new LinkedHashMap<>();

    public void replaceAll(Collection<CookingWorkstationType> definitions) {
        workstations.clear();
        if (definitions == null) return;
        for (CookingWorkstationType type : definitions) {
            if (type == null || type.id() == null || type.id().isBlank()) continue;
            workstations.put(type.id(), type);
        }
    }

    public Optional<CookingWorkstationType> get(String id) {
        return Optional.ofNullable(workstations.get(id));
    }

    public Collection<CookingWorkstationType> all() {
        return List.copyOf(workstations.values());
    }

    public int size() {
        return workstations.size();
    }
}
