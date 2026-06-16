package me.nakilex.levelplugin.cooking.stage;

import me.nakilex.levelplugin.cooking.model.CookingIngredientRequirement;
import me.nakilex.levelplugin.cooking.model.CookingStage;
import me.nakilex.levelplugin.cooking.model.CookingStageType;
import me.nakilex.levelplugin.cooking.runtime.ActiveCookingSession;
import me.nakilex.levelplugin.cooking.util.CookingIngredientMatcher;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.cooking.util.CookingChatMessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/** Executes INSERT_ITEM stages, including partial multi-ingredient progress. */
public class InsertItemStageExecutor implements CookingStageExecutor {

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
        context.controller().displayService().showInsertItemDisplays(
                session, stage, context.controller().recipe(session).orElse(null));
        CookingChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.INFO,
                "Cooking stage started. Insert " + ChatColor.YELLOW + context.controller().displayService().formatRequirements(stage, session.craftAmount())
                        + ChatColor.WHITE + " at the workstation.");
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
                CookingChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.WARNING,
                        "That ingredient is already complete for this stage.");
                return InteractionResult.INGREDIENT_ALREADY_COMPLETE;
            }
            context.controller().effectsService().playWrongIngredient(context.player(), context.rewardDropLocation());
            CookingChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.WARNING,
                    "That ingredient does not match this stage. Expected " + ChatColor.YELLOW
                            + context.controller().displayService().formatRequirements(stage, session.craftAmount()) + ChatColor.WHITE + ".");
            return InteractionResult.INVALID_INGREDIENT;
        }
        CookingIngredientRequirement requirement = requirementOptional.get();
        int insertAmount = Math.min(held.getAmount(), session.progress().remainingAmount(requirement, session.craftAmount()));
        if (insertAmount <= 0) {
            context.controller().effectsService().playWrongIngredient(context.player(), context.rewardDropLocation());
            CookingChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.WARNING,
                    "That ingredient is already complete for this stage.");
            return InteractionResult.INGREDIENT_ALREADY_COMPLETE;
        }

        if (context.player().getGameMode() != GameMode.CREATIVE) {
            removeFromMainHand(context.player(), held, insertAmount);
        }
        session.progress().addIngredient(requirement, insertAmount);
        context.controller().effectsService().playIngredientInserted(context.player(), context.rewardDropLocation());
        context.controller().displayService().updateInsertItemDisplays(session, stage);
        CookingChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.SUCCESS,
                "Inserted " + ChatColor.YELLOW + insertAmount + "x " + context.controller().displayService().formatRequirementName(requirement) + ChatColor.GREEN + ".");
        if (session.progress().areRequirementsComplete(stage, session.craftAmount())) {
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
                .filter(requirement -> !session.progress().isRequirementComplete(requirement, session.craftAmount()))
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
        return CookingIngredientMatcher.matches(requirement, stack);
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

}
