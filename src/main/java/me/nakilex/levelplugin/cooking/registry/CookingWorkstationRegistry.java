package me.nakilex.levelplugin.cooking.registry;

import me.nakilex.levelplugin.cooking.model.CookingWorkstationType;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/** In-memory registry for config-backed cooking workstation type definitions. */
public class CookingWorkstationRegistry {
    private final Map<String, CookingWorkstationType> workstations = new LinkedHashMap<>();
    private final Map<Material, List<CookingWorkstationType>> byBlockMaterial = new EnumMap<>(Material.class);

    public void replaceAll(Collection<CookingWorkstationType> definitions) {
        replaceAll(definitions, null);
    }

    public void replaceAll(Collection<CookingWorkstationType> definitions, Logger logger) {
        workstations.clear();
        byBlockMaterial.clear();
        if (definitions == null) return;
        for (CookingWorkstationType type : definitions) {
            if (type == null || type.id() == null || type.id().isBlank()) continue;
            workstations.put(type.id(), type);
            List<CookingWorkstationType> materialMatches = byBlockMaterial.computeIfAbsent(type.blockMaterial(), ignored -> new ArrayList<>());
            if (!materialMatches.isEmpty() && logger != null) {
                logger.warning("[Cooking] Multiple workstation types use block material " + type.blockMaterial()
                        + ": existing=" + materialMatches.stream().map(CookingWorkstationType::id).toList()
                        + ", new=" + type.id() + ". Placement will also compare the item used.");
            }
            materialMatches.add(type);
        }
    }

    public Optional<CookingWorkstationType> get(String id) {
        return Optional.ofNullable(workstations.get(id));
    }

    public Optional<CookingWorkstationType> findByBlockMaterial(Material material) {
        return findAllByBlockMaterial(material).stream().findFirst();
    }

    public List<CookingWorkstationType> findAllByBlockMaterial(Material material) {
        if (material == null) {
            return List.of();
        }
        return List.copyOf(byBlockMaterial.getOrDefault(material, List.of()));
    }

    public Collection<CookingWorkstationType> all() {
        return List.copyOf(workstations.values());
    }

    public int size() {
        return workstations.size();
    }
}
