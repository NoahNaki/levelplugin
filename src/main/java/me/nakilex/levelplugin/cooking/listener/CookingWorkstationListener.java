package me.nakilex.levelplugin.cooking.listener;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cooking.gui.CookingRecipeSelectionGUI;
import me.nakilex.levelplugin.cooking.registry.CookingWorkstationRegistry;
import me.nakilex.levelplugin.cooking.runtime.ActiveCookingSessionRegistry;
import me.nakilex.levelplugin.cooking.runtime.PlacedCookingWorkstation;
import me.nakilex.levelplugin.cooking.service.CookingSessionService;
import me.nakilex.levelplugin.cooking.runtime.PlacedCookingWorkstationRegistry;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Handles runtime placement tracking and recipe GUI access for cooking workstations. */
public class CookingWorkstationListener implements Listener {
    private final Main plugin;
    private final CookingWorkstationRegistry workstationTypes;
    private final PlacedCookingWorkstationRegistry placedWorkstations;
    private final ActiveCookingSessionRegistry activeSessions;
    private final CookingRecipeSelectionGUI recipeSelectionGUI;
    private final CookingSessionService sessionService;

    public CookingWorkstationListener(
            Main plugin,
            CookingWorkstationRegistry workstationTypes,
            PlacedCookingWorkstationRegistry placedWorkstations,
            ActiveCookingSessionRegistry activeSessions,
            CookingRecipeSelectionGUI recipeSelectionGUI,
            CookingSessionService sessionService
    ) {
        this.plugin = plugin;
        this.workstationTypes = workstationTypes;
        this.placedWorkstations = placedWorkstations;
        this.activeSessions = activeSessions;
        this.recipeSelectionGUI = recipeSelectionGUI;
        this.sessionService = sessionService;
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
            sessionService.cancelSessionByWorkstation(placed.locationKey());
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
            if (activeSessions.getByPlayer(event.getPlayer().getUniqueId()).isPresent()) {
                ChatMessageUtil.send(event.getPlayer(), ChatMessageUtil.MessageType.WARNING,
                        "You already have an active cooking session.");
                return;
            }
            if (activeSessions.getByWorkstation(placed.locationKey()).isPresent()) {
                ChatMessageUtil.send(event.getPlayer(), ChatMessageUtil.MessageType.WARNING,
                        "This cooking workstation is busy.");
                return;
            }
            recipeSelectionGUI.open(event.getPlayer(), placed);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        sessionService.cancelSessionByPlayer(event.getPlayer().getUniqueId());
    }
}
