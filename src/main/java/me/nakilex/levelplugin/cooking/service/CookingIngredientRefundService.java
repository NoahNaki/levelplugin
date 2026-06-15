package me.nakilex.levelplugin.cooking.service;

import me.nakilex.levelplugin.cooking.model.CookingIngredientRequirement;
import me.nakilex.levelplugin.cooking.model.CookingRecipe;
import me.nakilex.levelplugin.cooking.model.CookingStage;
import me.nakilex.levelplugin.cooking.model.CookingStageType;
import me.nakilex.levelplugin.cooking.runtime.ActiveCookingSession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/** Refunds already-inserted cooking ingredients when active sessions are cancelled during shutdown. */
public class CookingIngredientRefundService {
    public void refundInsertedIngredients(ActiveCookingSession session, CookingRecipe recipe, Logger logger) {
        if (session == null || recipe == null) {
            return;
        }
        Location dropLocation = resolveDropLocation(session);
        Player player = Bukkit.getPlayer(session.playerId());
        for (Integer stageIndex : session.progress().insertedByStageSnapshot().keySet()) {
            if (stageIndex == null || stageIndex < 0 || stageIndex >= recipe.stages().size()) {
                continue;
            }
            CookingStage stage = recipe.stages().get(stageIndex);
            if (stage.type() != CookingStageType.INSERT_ITEM) {
                continue;
            }
            refundStage(session, stageIndex, stage, player, dropLocation, logger);
        }
    }

    private void refundStage(ActiveCookingSession session,
                             int stageIndex,
                             CookingStage stage,
                             Player player,
                             Location dropLocation,
                             Logger logger) {
        Set<String> refundedKeys = new HashSet<>();
        for (CookingIngredientRequirement requirement : stage.requirements()) {
            if (!refundedKeys.add(requirement.progressKey())) {
                continue;
            }
            int insertedAmount = session.progress().insertedAmount(stageIndex, requirement);
            if (insertedAmount <= 0) {
                continue;
            }
            ItemStack refund = createItem(requirement, insertedAmount, logger);
            if (refund == null) {
                continue;
            }
            refundItem(player, dropLocation, refund);
        }
    }

    private ItemStack createItem(CookingIngredientRequirement requirement, int amount, Logger logger) {
        Material material = requirement.material();
        if (material == null || material.isAir()) {
            if (logger != null) {
                logger.warning("[Cooking] Could not refund inserted ingredient without a vanilla material: " + requirement.progressKey());
            }
            return null;
        }
        return new ItemStack(material, amount);
    }

    private void refundItem(Player player, Location dropLocation, ItemStack refund) {
        if (player != null && player.isOnline()) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(refund);
            for (ItemStack leftover : leftovers.values()) {
                dropItem(dropLocation != null ? dropLocation : player.getLocation(), leftover);
            }
            return;
        }
        dropItem(dropLocation, refund);
    }

    private void dropItem(Location location, ItemStack stack) {
        if (location == null || stack == null || stack.getType().isAir()) {
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.dropItemNaturally(location, stack);
    }

    private Location resolveDropLocation(ActiveCookingSession session) {
        Location location = session.workstationKey().toLocation();
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return location.clone().add(0.5, 0.5, 0.5);
    }
}
