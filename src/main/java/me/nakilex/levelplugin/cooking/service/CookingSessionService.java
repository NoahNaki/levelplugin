package me.nakilex.levelplugin.cooking.service;

import me.nakilex.levelplugin.cooking.model.CookingRecipe;
import me.nakilex.levelplugin.cooking.model.CookingStage;
import me.nakilex.levelplugin.cooking.model.CookingStageType;
import me.nakilex.levelplugin.cooking.registry.CookingRecipeRegistry;
import me.nakilex.levelplugin.cooking.runtime.ActiveCookingSession;
import me.nakilex.levelplugin.cooking.runtime.ActiveCookingSessionRegistry;
import me.nakilex.levelplugin.cooking.runtime.PlacedCookingWorkstation;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/** Owns active cooking session progression. Only INSERT_ITEM stages are implemented for now. */
public class CookingSessionService {
    private final CookingRecipeRegistry recipeRegistry;
    private final ActiveCookingSessionRegistry sessionRegistry;
    private final List<IngredientMatcher> ingredientMatchers = List.of(new VanillaMaterialIngredientMatcher());

    public CookingSessionService(CookingRecipeRegistry recipeRegistry, ActiveCookingSessionRegistry sessionRegistry) {
        this.recipeRegistry = recipeRegistry;
        this.sessionRegistry = sessionRegistry;
    }

    public ActiveCookingSessionRegistry.CreateResult startSession(Player player, PlacedCookingWorkstation workstation, CookingRecipe recipe) {
        ActiveCookingSessionRegistry.CreateResult result = sessionRegistry.create(
                player.getUniqueId(), workstation.locationKey(), recipe.id());
        if (result == ActiveCookingSessionRegistry.CreateResult.CREATED) {
            sessionRegistry.getByPlayer(player.getUniqueId()).ifPresent(session -> beginCurrentStage(player, session));
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

    public InsertResult insertHeldIngredient(Player player, PlacedCookingWorkstation workstation, ItemStack held) {
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
            sessionRegistry.removeByWorkstation(workstation.locationKey());
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Cooking recipe is no longer registered.");
            return InsertResult.INVALID_SESSION;
        }
        CookingRecipe recipe = recipeOptional.get();
        Optional<CookingStage> stageOptional = currentStage(session);
        if (stageOptional.isEmpty()) {
            complete(player, session, recipe);
            return InsertResult.COMPLETED;
        }
        CookingStage stage = stageOptional.get();
        if (stage.type() != CookingStageType.INSERT_ITEM) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "This cooking stage is not implemented yet.");
            return InsertResult.UNSUPPORTED_STAGE;
        }
        if (!matches(stage, held)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "That ingredient does not match this stage. Required: " + formatRequirement(stage) + ".");
            return InsertResult.INVALID_INGREDIENT;
        }
        if (held.getAmount() < stage.amount()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "You need " + stage.amount() + "x " + formatMaterial(stage.itemMaterial()) + ".");
            return InsertResult.NOT_ENOUGH_ITEMS;
        }

        removeFromMainHand(player, held, stage.amount());
        session.progress().advance();
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Inserted " + ChatColor.YELLOW + formatRequirement(stage) + ChatColor.GREEN + ".");
        advanceOrComplete(player, session, recipe);
        return InsertResult.ACCEPTED;
    }

    private void advanceOrComplete(Player player, ActiveCookingSession session, CookingRecipe recipe) {
        if (session.progress().currentStageIndex() >= recipe.stages().size()) {
            complete(player, session, recipe);
            return;
        }
        beginCurrentStage(player, session);
    }

    private void beginCurrentStage(Player player, ActiveCookingSession session) {
        Optional<CookingStage> stageOptional = currentStage(session);
        if (stageOptional.isEmpty()) {
            recipeRegistry.get(session.recipeId()).ifPresent(recipe -> complete(player, session, recipe));
            return;
        }
        CookingStage stage = stageOptional.get();
        if (stage.type() == CookingStageType.INSERT_ITEM) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Cooking stage started. Insert " + ChatColor.YELLOW + formatRequirement(stage)
                            + ChatColor.WHITE + " by right-clicking the workstation.");
            return;
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                "Next cooking stage is not implemented yet.");
    }

    private void complete(Player player, ActiveCookingSession session, CookingRecipe recipe) {
        sessionRegistry.removeByWorkstation(session.workstationKey());
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Completed cooking recipe " + ChatColor.YELLOW + recipe.displayName() + ChatColor.GREEN + ".");
    }

    private boolean matches(CookingStage stage, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        for (IngredientMatcher matcher : ingredientMatchers) {
            if (matcher.matches(stage, stack)) {
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

    private String formatRequirement(CookingStage stage) {
        return stage.amount() + "x " + formatMaterial(stage.itemMaterial());
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
        UNSUPPORTED_STAGE
    }

    /** Extension point for future custom item/Nexo ingredient matching. */
    private interface IngredientMatcher {
        boolean matches(CookingStage stage, ItemStack stack);
    }

    private static class VanillaMaterialIngredientMatcher implements IngredientMatcher {
        @Override
        public boolean matches(CookingStage stage, ItemStack stack) {
            return stage.itemMaterial() != null && stack.getType() == stage.itemMaterial();
        }
    }
}
