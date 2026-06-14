package me.nakilex.levelplugin.cooking.model;

import org.bukkit.Material;

import java.util.List;
import java.util.Optional;

/** Immutable config-backed cooking workstation type definition. */
public record CookingWorkstationType(
        String id,
        Material blockMaterial,
        Material itemMaterial,
        List<String> recipeIds,
        String permission
) {
    public CookingWorkstationType {
        recipeIds = List.copyOf(recipeIds == null ? List.of() : recipeIds);
        if (permission != null && permission.isBlank()) {
            permission = null;
        }
    }

    public Optional<String> permissionNode() {
        return Optional.ofNullable(permission);
    }
}
