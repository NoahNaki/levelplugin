package me.nakilex.levelplugin.cooking.service;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cooking.display.CookingDisplayAnimator;
import me.nakilex.levelplugin.cooking.minigame.CookingMiniGameVisual;
import me.nakilex.levelplugin.cooking.model.CookingIngredientRequirement;
import me.nakilex.levelplugin.cooking.model.CookingRecipe;
import me.nakilex.levelplugin.cooking.model.CookingStage;
import me.nakilex.levelplugin.cooking.runtime.ActiveCookingSession;
import me.nakilex.levelplugin.cooking.runtime.CookingDisplayState;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Owns floating cooking display entity lifecycle and display text formatting. */
public class CookingDisplayService {
    private static final ChatColor LABEL_COLOR = ChatColor.GRAY;
    private static final ChatColor NUMBER_COLOR = ChatColor.WHITE;
    private static final float INGREDIENT_SCALE = 0.55f;
    private static final float REWARD_SCALE = 0.72f;

    private final CookingDisplayAnimator animator;

    public CookingDisplayService(Main plugin) {
        this.animator = new CookingDisplayAnimator(plugin);
    }

    public void spawnDisplays(ActiveCookingSession session, CookingRecipe recipe) {
        if (session == null || recipe == null) {
            return;
        }
        cleanup(session);
        ensureDisplayState(session);
    }

    public void clearStageDisplays(ActiveCookingSession session) {
        if (session == null) {
            return;
        }
        CookingDisplayState state = ensureDisplayState(session);
        clearText(session);
        cleanupIngredientDisplays(state, false);
        cleanupManagedDisplay(state.rewardPreviewDisplay(), false);
        session.setDisplayState(state.withRewardPreviewDisplay(null));
    }

    public void showText(ActiveCookingSession session, String text) {
        if (session == null || text == null || text.isBlank()) {
            clearText(session);
            return;
        }
        CookingDisplayState state = ensureDisplayState(session);
        TextDisplay textDisplay = state.textDisplay();
        if (!isValid(textDisplay)) {
            textDisplay = spawnTextDisplay(session);
            session.setDisplayState(state.withTextDisplay(textDisplay));
        }
        if (isValid(textDisplay)) {
            textDisplay.setText(LABEL_COLOR + text);
        }
    }

    public void clearText(ActiveCookingSession session) {
        if (session == null || session.displayState() == null) {
            return;
        }
        TextDisplay textDisplay = session.displayState().textDisplay();
        safeRemove(textDisplay);
        session.setDisplayState(session.displayState().withTextDisplay(null));
    }

    public void clearIngredientDisplays(ActiveCookingSession session) {
        if (session == null || session.displayState() == null) {
            return;
        }
        cleanupIngredientDisplays(session.displayState(), false);
    }

    private CookingDisplayState ensureDisplayState(ActiveCookingSession session) {
        if (session.displayState() == null) {
            session.setDisplayState(new CookingDisplayState(null, null));
        }
        return session.displayState();
    }

    private TextDisplay spawnTextDisplay(ActiveCookingSession session) {
        Location workstationLocation = session.workstationKey().toLocation();
        if (workstationLocation == null || workstationLocation.getWorld() == null) {
            return null;
        }
        World world = workstationLocation.getWorld();
        TextDisplay textDisplay = world.spawn(workstationLocation.clone().add(0.5, 1.9, 0.5), TextDisplay.class);
        textDisplay.setBillboard(Display.Billboard.CENTER);
        textDisplay.setSeeThrough(false);
        return textDisplay;
    }

    public int showInsertItemDisplays(ActiveCookingSession session, CookingStage stage, CookingRecipe recipe) {
        if (session == null || stage == null) {
            return 0;
        }
        CookingDisplayState state = ensureDisplayState(session);
        Location workstationLocation = session.workstationKey().toLocation();
        if (workstationLocation == null || workstationLocation.getWorld() == null) {
            return 0;
        }
        cleanupIngredientDisplays(state, false);
        cleanupManagedDisplay(state.rewardPreviewDisplay(), false);
        session.setDisplayState(state.withRewardPreviewDisplay(null));

        Location base = workstationLocation.clone().add(0.5D, 1.3D, 0.5D);
        List<RequirementDisplayItem> remaining = remainingIngredientItems(session, stage);
        int spawned = spawnIngredientDisplays(state, base, remaining);
        if (recipe != null && !recipe.rewards().isEmpty()) {
            CookingDisplayState currentState = ensureDisplayState(session);
            currentState = currentState.withRewardPreviewDisplay(spawnRewardPreview(base, recipe.rewards().get(0).toItemStack()));
            session.setDisplayState(currentState);
        }
        if (stage.tooltip() != null) {
            showText(session, stage.tooltip());
        }
        return spawned;
    }

    public int updateInsertItemDisplays(ActiveCookingSession session, CookingStage stage) {
        if (session == null || stage == null || session.displayState() == null) {
            return 0;
        }
        Location workstationLocation = session.workstationKey().toLocation();
        if (workstationLocation == null || workstationLocation.getWorld() == null) {
            return 0;
        }
        CookingDisplayState state = session.displayState();
        cleanupIngredientDisplays(state, true);
        return spawnIngredientDisplays(state, workstationLocation.clone().add(0.5D, 1.3D, 0.5D), remainingIngredientItems(session, stage));
    }

    public int showIngredientDisplays(ActiveCookingSession session, CookingStage stage) {
        return showInsertItemDisplays(session, stage, null);
    }

    public void removeIngredientDisplay(ActiveCookingSession session, CookingIngredientRequirement requirement) {
        if (session == null || requirement == null || session.displayState() == null) {
            return;
        }
        CookingDisplayState.ManagedItemDisplay managed = session.displayState().ingredientDisplays().remove(requirement.progressKey());
        cleanupManagedDisplay(managed, true);
    }

    public void updateWaitProgress(ActiveCookingSession session, long secondsRemaining) {
        long safeSeconds = Math.max(0L, secondsRemaining);
        updateText(session, LABEL_COLOR + "Cooking...\n" + NUMBER_COLOR + safeSeconds + LABEL_COLOR + "s remaining");
    }

    public void showMiniGameVisual(Player player, CookingMiniGameVisual visual) {
        if (player == null || visual == null) {
            return;
        }
        player.sendTitle(visual.title(), visual.subtitle(), 0, 20, 0);
    }

    public void clearMiniGameVisual(Player player) {
        if (player == null) {
            return;
        }
        player.resetTitle();
        player.sendActionBar(net.kyori.adventure.text.Component.empty());
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
        cleanupIngredientDisplays(state, false);
        cleanupManagedDisplay(state.rewardPreviewDisplay(), false);
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

    private void updateText(ActiveCookingSession session, String text) {
        if (session == null) {
            return;
        }
        CookingDisplayState state = ensureDisplayState(session);
        TextDisplay textDisplay = state.textDisplay();
        if (!isValid(textDisplay)) {
            textDisplay = spawnTextDisplay(session);
            session.setDisplayState(state.withTextDisplay(textDisplay));
        }
        if (isValid(textDisplay)) {
            textDisplay.setText(text);
        }
    }

    private List<RequirementDisplayItem> remainingIngredientItems(ActiveCookingSession session, CookingStage stage) {
        List<RequirementDisplayItem> remaining = new ArrayList<>();
        for (CookingIngredientRequirement requirement : stage.requirements()) {
            int remainingAmount = session.progress().remainingAmount(requirement);
            if (remainingAmount > 0) {
                remaining.add(new RequirementDisplayItem(requirement.progressKey(), createRequirementDisplayItem(requirement, remainingAmount)));
            }
        }
        return remaining;
    }

    private int spawnIngredientDisplays(CookingDisplayState state, Location base, List<RequirementDisplayItem> items) {
        if (base.getWorld() == null || items == null || items.isEmpty()) {
            return 0;
        }
        double centerOffset = (items.size() - 1) / 2.0D;
        for (int index = 0; index < items.size(); index++) {
            RequirementDisplayItem item = items.get(index);
            Location location = base.clone().add((index - centerOffset) * 0.45D, 0.0D, 0.0D);
            ItemDisplay display = base.getWorld().spawn(location, ItemDisplay.class);
            display.setItemStack(item.stack());
            configureItemDisplay(display, INGREDIENT_SCALE);
            CookingDisplayAnimator.AnimatedDisplay animation = animator.animate(display, location, INGREDIENT_SCALE, 0.05D, 0.03D, 0.20D, 0.14D);
            cleanupManagedDisplay(state.ingredientDisplays().put(item.key(), new CookingDisplayState.ManagedItemDisplay(display, animation)), false);
        }
        return items.size();
    }

    private CookingDisplayState.ManagedItemDisplay spawnRewardPreview(Location ingredientBase, ItemStack reward) {
        if (ingredientBase == null || ingredientBase.getWorld() == null || reward == null || reward.getType().isAir()) {
            return null;
        }
        Location location = ingredientBase.clone().add(0.0D, 0.2D, 0.65D);
        ItemDisplay display = ingredientBase.getWorld().spawn(location, ItemDisplay.class);
        display.setItemStack(reward.clone());
        configureItemDisplay(display, REWARD_SCALE);
        CookingDisplayAnimator.AnimatedDisplay animation = animator.animate(display, location, REWARD_SCALE, 0.08D, 0.05D, 0.12D, 0.10D);
        return new CookingDisplayState.ManagedItemDisplay(display, animation);
    }

    private void cleanupIngredientDisplays(CookingDisplayState state, boolean animateRemoval) {
        for (CookingDisplayState.ManagedItemDisplay display : List.copyOf(state.ingredientDisplays().values())) {
            cleanupManagedDisplay(display, animateRemoval);
        }
        state.ingredientDisplays().clear();
    }

    private void cleanupManagedDisplay(CookingDisplayState.ManagedItemDisplay managed, boolean animateRemoval) {
        if (managed == null) {
            return;
        }
        if (animateRemoval) {
            animator.shrinkAndRemove(managed.animation());
            return;
        }
        animator.stop(managed.animation());
        safeRemove(managed.display());
    }

    private void configureItemDisplay(ItemDisplay display, float scale) {
        display.setBillboard(Display.Billboard.FIXED);
        display.setTransformation(new Transformation(
                new Vector3f(),
                new AxisAngle4f(),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f()));
    }

    private ItemStack createRequirementDisplayItem(CookingIngredientRequirement requirement, int amount) {
        Material material = requirement.material() == null || requirement.material().isAir() ? Material.STONE : requirement.material();
        ItemStack item = new ItemStack(material, Math.max(1, amount));
        requirement.nexoItemIdOptional().ifPresent(nexoItemId -> ItemUtil.applyNexoModel(item, nexoItemId));
        return item;
    }

    public void shutdownAnimations() {
        animator.stopAll();
    }

    private record RequirementDisplayItem(String key, ItemStack stack) {}


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
