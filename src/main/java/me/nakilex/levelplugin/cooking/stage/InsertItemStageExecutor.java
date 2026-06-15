package me.nakilex.levelplugin.cooking.stage;

import me.nakilex.levelplugin.cooking.model.CookingIngredientRequirement;
import me.nakilex.levelplugin.cooking.model.CookingStage;
import me.nakilex.levelplugin.cooking.model.CookingStageType;
import me.nakilex.levelplugin.cooking.runtime.ActiveCookingSession;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/** Executes INSERT_ITEM stages, including partial multi-ingredient progress. */
public class InsertItemStageExecutor implements CookingStageExecutor {
    private final List<IngredientMatcher> ingredientMatchers = List.of(new VanillaMaterialIngredientMatcher());

    @Override
    public CookingStageType type() {
        return CookingStageType.INSERT_ITEM;
    }

    @Override
    public void beginStage(ActiveCookingSession session, StageExecutionContext context) {
        CookingStage stage = context.controller().currentStage(session).orElse(null);
        if (stage == null) {
            context.controller().advanceStage(context.player(), session, context.rewardDropLocation());
            return;
        }
        context.controller().displayService().clearStageDisplays(session);
        int ingredientDisplayCount = context.controller().displayService().showIngredientDisplays(session, stage);
        context.controller().plugin().getLogger().info("[Cooking] Spawned ingredient displays count="
                + ingredientDisplayCount + " recipe=" + session.recipeId());
        ChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.INFO,
                "Cooking stage started. Insert " + ChatColor.YELLOW + context.controller().displayService().formatRequirements(stage)
                        + ChatColor.WHITE + " by right-clicking the workstation.");
    }

    @Override
    public InteractionResult handleInteraction(ActiveCookingSession session, StageInteractionContext context) {
        CookingStage stage = context.controller().currentStage(session).orElse(null);
        if (stage == null) {
            context.controller().advanceStage(context.player(), session, context.rewardDropLocation());
            return InteractionResult.COMPLETED;
        }
        ItemStack held = context.heldItem();
        Optional<CookingIngredientRequirement> requirementOptional = findInsertableRequirement(stage, session, held);
        if (requirementOptional.isEmpty()) {
            if (matchesAnyRequirement(stage, held)) {
                context.controller().effectsService().playWrongIngredient(context.player(), context.rewardDropLocation());
                ChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.WARNING,
                        "That ingredient is already complete for this stage.");
                return InteractionResult.INGREDIENT_ALREADY_COMPLETE;
            }
            context.controller().effectsService().playWrongIngredient(context.player(), context.rewardDropLocation());
            ChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.WARNING,
                    "That ingredient does not match this stage. Required: " + context.controller().displayService().formatRequirements(stage) + ".");
            return InteractionResult.INVALID_INGREDIENT;
        }
        CookingIngredientRequirement requirement = requirementOptional.get();
        int insertAmount = Math.min(held.getAmount(), session.progress().remainingAmount(requirement));
        if (insertAmount <= 0) {
            context.controller().effectsService().playWrongIngredient(context.player(), context.rewardDropLocation());
            ChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.WARNING,
                    "That ingredient is already complete for this stage.");
            return InteractionResult.INGREDIENT_ALREADY_COMPLETE;
        }

        if (context.player().getGameMode() != GameMode.CREATIVE) {
            removeFromMainHand(context.player(), held, insertAmount);
        }
        session.progress().addIngredient(requirement, insertAmount);
        context.controller().effectsService().playIngredientInserted(context.player(), context.rewardDropLocation());
        if (session.progress().isRequirementComplete(requirement)) {
            context.controller().displayService().removeIngredientDisplay(session, requirement);
        }
        ChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.SUCCESS,
                "Inserted " + ChatColor.YELLOW + insertAmount + "x " + context.controller().displayService().formatRequirementName(requirement) + ChatColor.GREEN + ".");
        if (session.progress().areRequirementsComplete(stage)) {
            completeStage(session, new StageExecutionContext(context.controller(), context.player(), context.rewardDropLocation()));
        }
        return InteractionResult.ACCEPTED;
    }

    @Override
    public void cancelStage(ActiveCookingSession session) {
        // INSERT_ITEM stages do not own scheduled resources yet.
    }

    @Override
    public void completeStage(ActiveCookingSession session, StageExecutionContext context) {
        session.progress().advance();
        context.controller().advanceStage(context.player(), session, context.rewardDropLocation());
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
