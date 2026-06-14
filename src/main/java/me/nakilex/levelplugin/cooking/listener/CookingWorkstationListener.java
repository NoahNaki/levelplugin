package me.nakilex.levelplugin.cooking.listener;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cooking.model.CookingWorkstationType;
import me.nakilex.levelplugin.cooking.registry.CookingWorkstationRegistry;
import me.nakilex.levelplugin.cooking.runtime.PlacedCookingWorkstation;
import me.nakilex.levelplugin.cooking.runtime.PlacedCookingWorkstationRegistry;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;

import java.util.StringJoiner;

/** Handles runtime placement tracking for cooking workstations. Does not start recipes yet. */
public class CookingWorkstationListener implements Listener {
    private final Main plugin;
    private final CookingWorkstationRegistry workstationTypes;
    private final PlacedCookingWorkstationRegistry placedWorkstations;

    public CookingWorkstationListener(
            Main plugin,
            CookingWorkstationRegistry workstationTypes,
            PlacedCookingWorkstationRegistry placedWorkstations
    ) {
        this.plugin = plugin;
        this.workstationTypes = workstationTypes;
        this.placedWorkstations = placedWorkstations;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        workstationTypes.findByBlockMaterial(block.getType()).ifPresent(type -> {
            PlacedCookingWorkstation placed = placedWorkstations.register(block, type, event.getPlayer().getUniqueId());
            plugin.getLogger().info("[Cooking] Registered placed workstation '" + type.id()
                    + "' at " + placed.locationKey() + " by " + event.getPlayer().getName() + ".");
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        placedWorkstations.unregister(event.getBlock()).ifPresent(placed -> {
            // TODO: Stop and clean up an active cooking session at this workstation when sessions exist.
            plugin.getLogger().info("[Cooking] Unregistered placed workstation '" + placed.type().id()
                    + "' at " + placed.locationKey() + " after block break by " + event.getPlayer().getName() + ".");
        });
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }
        placedWorkstations.find(clicked).ifPresent(placed -> {
            event.setCancelled(true);
            ChatMessageUtil.send(
                    event.getPlayer(),
                    ChatMessageUtil.MessageType.INFO,
                    "Cooking workstation detected. Available recipes: "
                            + ChatColor.YELLOW + recipeList(placed.type()) + ChatColor.WHITE + ".");
        });
    }

    private String recipeList(CookingWorkstationType type) {
        if (type.recipeIds().isEmpty()) {
            return "none";
        }
        StringJoiner joiner = new StringJoiner(ChatColor.GRAY + ", " + ChatColor.YELLOW);
        for (String recipeId : type.recipeIds()) {
            joiner.add(recipeId);
        }
        return joiner.toString();
    }
}
