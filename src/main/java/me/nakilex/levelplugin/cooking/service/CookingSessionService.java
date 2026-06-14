package me.nakilex.levelplugin.cooking.service;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cooking.model.CookingIngredientRequirement;
import me.nakilex.levelplugin.cooking.model.CookingRecipe;
import me.nakilex.levelplugin.cooking.model.CookingStage;
import me.nakilex.levelplugin.cooking.model.CookingStageType;
import me.nakilex.levelplugin.cooking.registry.CookingRecipeRegistry;
import me.nakilex.levelplugin.cooking.runtime.ActiveCookingSession;
import me.nakilex.levelplugin.cooking.runtime.ActiveCookingSessionRegistry;
import me.nakilex.levelplugin.cooking.runtime.CookingWaitTask;
import me.nakilex.levelplugin.cooking.runtime.PlacedCookingWorkstation;
import me.nakilex.levelplugin.cooking.runtime.PlacedCookingWorkstationRegistry;
import me.nakilex.levelplugin.cooking.util.CookingLocationKey;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/** Owns active cooking session progression. INSERT_ITEM and WAIT stages are implemented for now. */
public class CookingSessionService {
    private static final long WAIT_TICK_PERIOD = 20L;

    private final Main plugin;
    private final CookingRecipeRegistry recipeRegistry;
    private final ActiveCookingSessionRegistry sessionRegistry;
    private final PlacedCookingWorkstationRegistry placedWorkstations;
    private final CookingRewardService rewardService;
    private final List<IngredientMatcher> ingredientMatchers = List.of(new VanillaMaterialIngredientMatcher());

    public CookingSessionService(Main plugin,
                                 CookingRecipeRegistry recipeRegistry,
                                 ActiveCookingSessionRegistry sessionRegistry,
                                 PlacedCookingWorkstationRegistry placedWorkstations) {
        this(plugin, recipeRegistry, sessionRegistry, placedWorkstations, new CookingRewardService());
    }

    public CookingSessionService(Main plugin,
                                 CookingRecipeRegistry recipeRegistry,
                                 ActiveCookingSessionRegistry sessionRegistry,
                                 PlacedCookingWorkstationRegistry placedWorkstations,
                                 CookingRewardService rewardService) {
        this.plugin = plugin;
        this.recipeRegistry = recipeRegistry;
        this.sessionRegistry = sessionRegistry;
        this.placedWorkstations = placedWorkstations;
        this.rewardService = rewardService;
    }

    public ActiveCookingSessionRegistry.CreateResult startSession(Player player, PlacedCookingWorkstation workstation, CookingRecipe recipe) {
        ActiveCookingSessionRegistry.CreateResult result = sessionRegistry.create(
                player.getUniqueId(), workstation.locationKey(), recipe.id());
        if (result == ActiveCookingSessionRegistry.CreateResult.CREATED) {
            sessionRegistry.getByPlayer(player.getUniqueId()).ifPresent(session -> {
                spawnDisplayEntities(session, recipe);
                beginCurrentStage(player, session);
            });
        }
        return result;
    }

    public Optional<CookingStage> currentStage(ActiveCookingSession session) {
        if (session == null) {
            return Optional.empty();
        }
        return recipeRegistry.get(session.recipeId())
                .filter(recipe -> session.progress().currentStageIndex() < recipe.stages().size())
                .map(recipe -> recipe.stages().get(session.progress().currentStageIndex()));
    }

    public InsertResult insertHeldIngredient(Player player, PlacedCookingWorkstation workstation, ItemStack held, Location rewardDropLocation) {
        Optional<ActiveCookingSession> sessionOptional = sessionRegistry.getByWorkstation(workstation.locationKey());
        if (sessionOptional.isEmpty()) {
            return InsertResult.NO_ACTIVE_SESSION;
        }
        ActiveCookingSession session = sessionOptional.get();
        if (!session.playerId().equals(player.getUniqueId())) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "This cooking workstation is busy.");
            return InsertResult.WRONG_PLAYER;
        }
        Optional<CookingRecipe> recipeOptional = recipeRegistry.get(session.recipeId());
        if (recipeOptional.isEmpty()) {
            cancelSession(session, "Cooking recipe is no longer registered.");
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Cooking recipe is no longer registered.");
            return InsertResult.INVALID_SESSION;
        }
        CookingRecipe recipe = recipeOptional.get();
        Optional<CookingStage> stageOptional = currentStage(session);
        if (stageOptional.isEmpty()) {
            complete(player, session, recipe, rewardDropLocation);
            return InsertResult.COMPLETED;
        }
        CookingStage stage = stageOptional.get();
        if (stage.type() != CookingStageType.INSERT_ITEM) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "This stage does not accept ingredient insertion right now.");
            return InsertResult.UNSUPPORTED_STAGE;
        }
        Optional<CookingIngredientRequirement> requirementOptional = findInsertableRequirement(stage, session, held);
        if (requirementOptional.isEmpty()) {
            if (matchesAnyRequirement(stage, held)) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "That ingredient is already complete for this stage.");
                return InsertResult.INGREDIENT_ALREADY_COMPLETE;
            }
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "That ingredient does not match this stage. Required: " + formatRequirements(stage) + ".");
            return InsertResult.INVALID_INGREDIENT;
        }
        CookingIngredientRequirement requirement = requirementOptional.get();
        int insertAmount = Math.min(held.getAmount(), session.progress().remainingAmount(requirement));
        if (insertAmount <= 0) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "That ingredient is already complete for this stage.");
            return InsertResult.INGREDIENT_ALREADY_COMPLETE;
        }

        if (player.getGameMode() != GameMode.CREATIVE) {
            removeFromMainHand(player, held, insertAmount);
        }
        session.progress().addIngredient(requirement, insertAmount);
        updateInsertDisplay(session, stage);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Inserted " + ChatColor.YELLOW + insertAmount + "x " + formatRequirementName(requirement) + ChatColor.GREEN + ".");
        if (session.progress().areRequirementsComplete(stage)) {
            session.progress().advance();
            advanceOrComplete(player, session, recipe, rewardDropLocation);
        }
        return InsertResult.ACCEPTED;
    }

    public void cancelSessionByPlayer(java.util.UUID playerId) {
        sessionRegistry.getByPlayer(playerId).ifPresent(session -> cancelSession(session, null));
    }

    public void cancelSessionByWorkstation(CookingLocationKey workstationKey) {
        sessionRegistry.getByWorkstation(workstationKey).ifPresent(session -> cancelSession(session, null));
    }

    private void advanceOrComplete(Player player, ActiveCookingSession session, CookingRecipe recipe, Location rewardDropLocation) {
        if (session.progress().currentStageIndex() >= recipe.stages().size()) {
            complete(player, session, recipe, rewardDropLocation);
            return;
        }
        beginCurrentStage(player, session);
    }

    private void beginCurrentStage(Player player, ActiveCookingSession session) {
        Optional<CookingStage> stageOptional = currentStage(session);
        if (stageOptional.isEmpty()) {
            recipeRegistry.get(session.recipeId()).ifPresent(recipe -> complete(player, session, recipe, player.getLocation()));
            return;
        }
        CookingStage stage = stageOptional.get();
        if (stage.type() == CookingStageType.INSERT_ITEM) {
            updateInsertDisplay(session, stage);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Cooking stage started. Insert " + ChatColor.YELLOW + formatRequirements(stage)
                            + ChatColor.WHITE + " by right-clicking the workstation.");
            return;
        }
        if (stage.type() == CookingStageType.WAIT) {
            startWaitStage(player, session, stage);
            return;
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                "Next cooking stage is not implemented yet.");
    }

    private void startWaitStage(Player player, ActiveCookingSession session, CookingStage stage) {
        long durationTicks = Math.max(1L, stage.durationTicks());
        session.updateTextDisplay("Cooking...");
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Cooking timer started for " + Math.ceil(durationTicks / 20.0D) + "s.");
        CookingWaitTask waitTask = new CookingWaitTask(
                durationTicks,
                WAIT_TICK_PERIOD,
                () -> isWaitStageStillValid(session),
                remainingTicks -> updateWaitDisplay(session, remainingTicks),
                () -> finishWaitStage(session),
                () -> cancelSession(session, null)
        );
        BukkitTask task = waitTask.runTaskTimer(plugin, 0L, WAIT_TICK_PERIOD);
        session.setWaitTask(task);
    }

    private boolean isWaitStageStillValid(ActiveCookingSession session) {
        return sessionRegistry.getByWorkstation(session.workstationKey()).filter(active -> active == session).isPresent()
                && placedWorkstations.find(session.workstationKey()).isPresent();
    }

    private void updateWaitDisplay(ActiveCookingSession session, long remainingTicks) {
        long secondsLeft = (long) Math.ceil(remainingTicks / 20.0D);
        session.updateTextDisplay("Cooking... " + secondsLeft + "s");
    }

    private void finishWaitStage(ActiveCookingSession session) {
        session.cancelWaitTask();
        Player player = Bukkit.getPlayer(session.playerId());
        if (player == null || !player.isOnline()) {
            cancelSession(session, null);
            return;
        }
        Optional<CookingRecipe> recipeOptional = recipeRegistry.get(session.recipeId());
        if (recipeOptional.isEmpty()) {
            cancelSession(session, "Cooking recipe is no longer registered.");
            return;
        }
        CookingRecipe recipe = recipeOptional.get();
        session.progress().advance();
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Cooking timer complete.");
        advanceOrComplete(player, session, recipe, session.workstationKey().toLocation());
    }

    private void complete(Player player, ActiveCookingSession session, CookingRecipe recipe, Location rewardDropLocation) {
        rewardService.grantRewards(player, rewardDropLocation, recipe.rewards());
        cleanupSession(session);
        sessionRegistry.removeByWorkstation(session.workstationKey());
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Completed cooking recipe " + ChatColor.YELLOW + recipe.displayName() + ChatColor.GREEN + ".");
    }

    private void cancelSession(ActiveCookingSession session, String logReason) {
        cleanupSession(session);
        sessionRegistry.removeByWorkstation(session.workstationKey());
        if (logReason != null && !logReason.isBlank()) {
            plugin.getLogger().warning("[Cooking] Cancelled session for recipe '" + session.recipeId() + "': " + logReason);
        }
    }

    private void cleanupSession(ActiveCookingSession session) {
        session.cancelWaitTask();
        session.removeDisplayEntities();
    }

    private void spawnDisplayEntities(ActiveCookingSession session, CookingRecipe recipe) {
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
        session.attachDisplayEntities(itemDisplay, textDisplay);
    }

    private Optional<CookingIngredientRequirement> findInsertableRequirement(CookingStage stage, ActiveCookingSession session, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return Optional.empty();
        }
        return stage.requirements().stream()
                .filter(requirement -> !session.progress().isRequirementComplete(requirement))
                .filter(requirement -> matches(requirement, stack))
                .findFirst();
    }

    private boolean matchesAnyRequirement(CookingStage stage, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        return stage.requirements().stream().anyMatch(requirement -> matches(requirement, stack));
    }

    private boolean matches(CookingIngredientRequirement requirement, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        for (IngredientMatcher matcher : ingredientMatchers) {
            if (matcher.matches(requirement, stack)) {
                return true;
            }
        }
        return false;
    }

    private void removeFromMainHand(Player player, ItemStack held, int amount) {
        int remaining = held.getAmount() - amount;
        if (remaining <= 0) {
            player.getInventory().setItemInMainHand(null);
            return;
        }
        held.setAmount(remaining);
        player.getInventory().setItemInMainHand(held);
    }

    private void updateInsertDisplay(ActiveCookingSession session, CookingStage stage) {
        List<String> lines = stage.requirements().stream()
                .map(requirement -> {
                    int inserted = session.progress().insertedAmount(requirement);
                    int required = requirement.amount();
                    String prefix = inserted >= required ? "✓" : "•";
                    return prefix + " " + formatRequirementName(requirement) + " " + Math.min(inserted, required) + "/" + required;
                })
                .toList();
        session.updateTextDisplay(String.join("\n", lines));
    }

    private String formatRequirements(CookingStage stage) {
        return stage.requirements().stream()
                .map(this::formatRequirement)
                .reduce((left, right) -> left + ", " + right)
                .orElse("Unknown");
    }

    private String formatRequirement(CookingIngredientRequirement requirement) {
        return requirement.amount() + "x " + formatRequirementName(requirement);
    }

    private String formatRequirementName(CookingIngredientRequirement requirement) {
        return requirement.nexoItemIdOptional().map(id -> "Nexo " + id).orElseGet(() -> formatMaterial(requirement.material()));
    }

    private String formatMaterial(Material material) {
        if (material == null) {
            return "Unknown";
        }
        String lower = material.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    public enum InsertResult {
        ACCEPTED,
        COMPLETED,
        NO_ACTIVE_SESSION,
        WRONG_PLAYER,
        INVALID_SESSION,
        INVALID_INGREDIENT,
        NOT_ENOUGH_ITEMS,
        INGREDIENT_ALREADY_COMPLETE,
        UNSUPPORTED_STAGE
    }

    /** Extension point for future custom item/Nexo ingredient matching. */
    private interface IngredientMatcher {
        boolean matches(CookingIngredientRequirement requirement, ItemStack stack);
    }

    private static class VanillaMaterialIngredientMatcher implements IngredientMatcher {
        @Override
        public boolean matches(CookingIngredientRequirement requirement, ItemStack stack) {
            String expectedNexo = requirement.nexoItemId();
            if (expectedNexo != null && !expectedNexo.isBlank()) {
                return expectedNexo.equalsIgnoreCase(ItemUtil.getNexoModelId(stack));
            }
            return requirement.material() != null && stack.getType() == requirement.material();
        }
    }
}
