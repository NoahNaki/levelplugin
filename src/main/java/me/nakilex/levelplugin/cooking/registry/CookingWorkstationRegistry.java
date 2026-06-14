package me.nakilex.levelplugin.cooking.registry;

import me.nakilex.levelplugin.cooking.model.CookingWorkstationType;
import org.bukkit.Material;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory registry for config-backed cooking workstation type definitions. */
public class CookingWorkstationRegistry {
    private final Map<String, CookingWorkstationType> workstations = new LinkedHashMap<>();
    private final Map<Material, CookingWorkstationType> byBlockMaterial = new EnumMap<>(Material.class);

    public void replaceAll(Collection<CookingWorkstationType> definitions) {
        workstations.clear();
        byBlockMaterial.clear();
        if (definitions == null) return;
        for (CookingWorkstationType type : definitions) {
            if (type == null || type.id() == null || type.id().isBlank()) continue;
            workstations.put(type.id(), type);
            byBlockMaterial.put(type.blockMaterial(), type);
        }
    }

    public Optional<CookingWorkstationType> get(String id) {
        return Optional.ofNullable(workstations.get(id));
    }

    public Optional<CookingWorkstationType> findByBlockMaterial(Material material) {
        return Optional.ofNullable(material == null ? null : byBlockMaterial.get(material));
    }

    public Collection<CookingWorkstationType> all() {
        return List.copyOf(workstations.values());
    }

    public int size() {
        return workstations.size();
    }
}
