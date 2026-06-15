package me.nakilex.levelplugin.cooking.service;

import me.nakilex.levelplugin.cooking.model.CookingIngredientRequirement;
import me.nakilex.levelplugin.cooking.model.CookingRecipe;
import me.nakilex.levelplugin.cooking.model.CookingStage;
import me.nakilex.levelplugin.cooking.runtime.ActiveCookingSession;
import me.nakilex.levelplugin.cooking.runtime.CookingDisplayState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.List;

/** Owns floating cooking display entity lifecycle and display text formatting. */
public class CookingDisplayService {
    public void spawnDisplays(ActiveCookingSession session, CookingRecipe recipe) {
        if (session == null || recipe == null) {
            return;
        }
        cleanup(session);
        Location workstationLocation = session.workstationKey().toLocation();
        if (workstationLocation == null || workstationLocation.getWorld() == null) {
            return;
        }
        World world = workstationLocation.getWorld();
        Location displayLocation = workstationLocation.clone().add(0.5, 1.25, 0.5);
        ItemDisplay itemDisplay = world.spawn(displayLocation, ItemDisplay.class);
        itemDisplay.setItemStack(recipe.displayItem());
        itemDisplay.setBillboard(Display.Billboard.FIXED);
        itemDisplay.setTransformation(new Transformation(
                new Vector3f(),
                new AxisAngle4f(),
                new Vector3f(0.6f, 0.6f, 0.6f),
                new AxisAngle4f()));

        TextDisplay textDisplay = world.spawn(displayLocation.clone().add(0, 0.35, 0), TextDisplay.class);
        textDisplay.setText("Cooking...");
        textDisplay.setBillboard(Display.Billboard.CENTER);
        textDisplay.setSeeThrough(false);
        session.setDisplayState(new CookingDisplayState(itemDisplay, textDisplay));
    }

    public void updateIngredientProgress(ActiveCookingSession session, CookingStage stage) {
        if (session == null || stage == null) {
            return;
        }
        updateText(session, formatIngredientProgress(session, stage));
    }

    public void updateWaitProgress(ActiveCookingSession session, long secondsRemaining) {
        long safeSeconds = Math.max(0L, secondsRemaining);
        updateText(session, "Cooking...\n" + safeSeconds + "s remaining");
    }

    public void cleanup(ActiveCookingSession session) {
        if (session == null) {
            return;
        }
        CookingDisplayState state = session.displayState();
        if (state == null) {
            return;
        }
        safeRemove(state.itemDisplay());
        safeRemove(state.textDisplay());
        session.clearDisplayState();
    }

    public String formatRequirements(CookingStage stage) {
        if (stage == null) {
            return "Unknown";
        }
        return stage.requirements().stream()
                .map(this::formatRequirement)
                .reduce((left, right) -> left + ", " + right)
                .orElse("Unknown");
    }

    public String formatRequirement(CookingIngredientRequirement requirement) {
        return requirement.amount() + "x " + formatRequirementName(requirement);
    }

    public String formatRequirementName(CookingIngredientRequirement requirement) {
        return requirement.nexoItemIdOptional().map(id -> "Nexo " + id).orElseGet(() -> formatMaterial(requirement.material()));
    }

    private String formatIngredientProgress(ActiveCookingSession session, CookingStage stage) {
        List<String> lines = stage.requirements().stream()
                .map(requirement -> {
                    int inserted = session.progress().insertedAmount(requirement);
                    int required = requirement.amount();
                    String prefix = inserted >= required ? "✓" : "•";
                    return prefix + " " + formatRequirementName(requirement) + " " + Math.min(inserted, required) + "/" + required;
                })
                .toList();
        return String.join("\n", lines);
    }

    private void updateText(ActiveCookingSession session, String text) {
        CookingDisplayState state = session.displayState();
        if (state != null && isValid(state.textDisplay())) {
            state.textDisplay().setText(text);
        }
    }

    private void safeRemove(Entity entity) {
        if (isValid(entity)) {
            entity.remove();
        }
    }

    private boolean isValid(Entity entity) {
        return entity != null && entity.isValid();
    }

    private String formatMaterial(Material material) {
        if (material == null) {
            return "Unknown";
        }
        String lower = material.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
