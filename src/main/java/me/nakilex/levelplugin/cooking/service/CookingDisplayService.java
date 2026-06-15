package me.nakilex.levelplugin.cooking.service;

import me.nakilex.levelplugin.cooking.model.CookingIngredientRequirement;
import me.nakilex.levelplugin.cooking.model.CookingRecipe;
import me.nakilex.levelplugin.cooking.model.CookingStage;
import me.nakilex.levelplugin.cooking.runtime.ActiveCookingSession;
import me.nakilex.levelplugin.cooking.runtime.CookingDisplayState;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.ChatColor;
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
    private static final String GREEN_CHECK = ChatColor.GREEN + "\u2714";
    private static final String RED_CROSS = ChatColor.RED + "\u2718";
    private static final ChatColor LABEL_COLOR = ChatColor.GRAY;
    private static final ChatColor NUMBER_COLOR = ChatColor.WHITE;

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
        configureItemDisplay(itemDisplay, 0.6f);

        TextDisplay textDisplay = world.spawn(displayLocation.clone().add(0, 0.35, 0), TextDisplay.class);
        textDisplay.setText("Cooking...");
        textDisplay.setBillboard(Display.Billboard.CENTER);
        textDisplay.setSeeThrough(false);
        session.setDisplayState(new CookingDisplayState(itemDisplay, textDisplay));
    }

    public void showIngredientDisplays(ActiveCookingSession session, CookingStage stage) {
        if (session == null || stage == null) {
            return;
        }
        CookingDisplayState state = session.displayState();
        Location workstationLocation = session.workstationKey().toLocation();
        if (state == null || workstationLocation == null || workstationLocation.getWorld() == null) {
            return;
        }
        cleanupIngredientDisplays(state);
        World world = workstationLocation.getWorld();
        List<CookingIngredientRequirement> requirements = stage.requirements();
        double centerOffset = (requirements.size() - 1) / 2.0D;
        for (int index = 0; index < requirements.size(); index++) {
            CookingIngredientRequirement requirement = requirements.get(index);
            Location displayLocation = workstationLocation.clone().add(0.5 + ((index - centerOffset) * 0.45D), 1.85D, 0.5D);
            ItemDisplay ingredientDisplay = world.spawn(displayLocation, ItemDisplay.class);
            ingredientDisplay.setItemStack(createRequirementDisplayItem(requirement));
            configureItemDisplay(ingredientDisplay, 0.35f);
            safeRemove(state.ingredientDisplays().put(requirement.progressKey(), ingredientDisplay));
        }
    }

    public void removeIngredientDisplay(ActiveCookingSession session, CookingIngredientRequirement requirement) {
        if (session == null || requirement == null || session.displayState() == null) {
            return;
        }
        ItemDisplay display = session.displayState().ingredientDisplays().remove(requirement.progressKey());
        safeRemove(display);
    }

    public void updateIngredientProgress(ActiveCookingSession session, CookingStage stage) {
        if (session == null || stage == null) {
            return;
        }
        updateText(session, formatIngredientProgress(session, stage));
    }

    public void updateWaitProgress(ActiveCookingSession session, long secondsRemaining) {
        long safeSeconds = Math.max(0L, secondsRemaining);
        updateText(session, LABEL_COLOR + "Cooking...\n" + NUMBER_COLOR + safeSeconds + LABEL_COLOR + "s remaining");
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
        cleanupIngredientDisplays(state);
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
                    boolean complete = inserted >= required;
                    String icon = complete ? GREEN_CHECK : RED_CROSS;
                    return icon
                            + LABEL_COLOR + " " + formatRequirementName(requirement)
                            + " " + NUMBER_COLOR + Math.min(inserted, required)
                            + LABEL_COLOR + "/" + NUMBER_COLOR + required;
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

    private void cleanupIngredientDisplays(CookingDisplayState state) {
        for (ItemDisplay display : List.copyOf(state.ingredientDisplays().values())) {
            safeRemove(display);
        }
        state.ingredientDisplays().clear();
    }

    private void configureItemDisplay(ItemDisplay display, float scale) {
        display.setBillboard(Display.Billboard.FIXED);
        display.setTransformation(new Transformation(
                new Vector3f(),
                new AxisAngle4f(),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f()));
    }

    private org.bukkit.inventory.ItemStack createRequirementDisplayItem(CookingIngredientRequirement requirement) {
        Material material = requirement.material() == null || requirement.material().isAir() ? Material.STONE : requirement.material();
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(material);
        requirement.nexoItemIdOptional().ifPresent(nexoItemId -> ItemUtil.applyNexoModel(item, nexoItemId));
        return item;
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
