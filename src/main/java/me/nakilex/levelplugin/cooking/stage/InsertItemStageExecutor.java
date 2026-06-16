package me.nakilex.levelplugin.cooking.stage;

import me.nakilex.levelplugin.cooking.model.CookingIngredientRequirement;
import me.nakilex.levelplugin.cooking.model.CookingStage;
import me.nakilex.levelplugin.cooking.model.CookingStageType;
import me.nakilex.levelplugin.cooking.runtime.ActiveCookingSession;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.TextUtil;
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
        context.controller().displayService().showInsertItemDisplays(
                session, stage, context.controller().recipe(session).orElse(null));
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
                    "That ingredient does not match this stage. Expected " + ChatColor.YELLOW
                            + context.controller().displayService().formatRequirements(stage) + ChatColor.WHITE + ".");
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
        context.controller().displayService().updateInsertItemDisplays(session, stage);
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
                return matchesNexoRequirement(expectedNexo, stack);
            }
            return requirement.material() != null && stack.getType() == requirement.material();
        }

        private boolean matchesNexoRequirement(String expectedNexo, ItemStack stack) {
            String modelId = ItemUtil.getNexoModelId(stack);
            if (expectedNexo.equalsIgnoreCase(modelId)) {
                return true;
            }
            if (matchesDisplayName(expectedNexo, stack)) {
                return true;
            }
            return matchesNexoVisualModel(expectedNexo, stack);
        }

        private boolean matchesDisplayName(String expectedNexo, ItemStack stack) {
            org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) {
                return false;
            }
            String displayName = ChatColor.stripColor(meta.getDisplayName());
            return normalizeName(TextUtil.beautifyWords(expectedNexo)).equals(normalizeName(displayName));
        }

        private boolean matchesNexoVisualModel(String expectedNexo, ItemStack stack) {
            com.nexomc.nexo.items.ItemBuilder builder = com.nexomc.nexo.api.NexoItems.itemFromId(expectedNexo);
            if (builder == null) {
                return false;
            }
            ItemStack expected = builder.build();
            if (expected == null || expected.getType() != stack.getType()) {
                return false;
            }
            org.bukkit.inventory.meta.ItemMeta expectedMeta = expected.getItemMeta();
            org.bukkit.inventory.meta.ItemMeta actualMeta = stack.getItemMeta();
            if (expectedMeta == null || actualMeta == null || !expectedMeta.hasCustomModelData() || !actualMeta.hasCustomModelData()) {
                return false;
            }
            return expectedMeta.getCustomModelData() == actualMeta.getCustomModelData();
        }

        private String normalizeName(String name) {
            return name == null ? "" : name.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
        }
    }
}
