package me.nakilex.levelplugin.cooking.listener;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cooking.gui.CookingRecipeSelectionGUI;
import me.nakilex.levelplugin.cooking.persistence.CookingWorkstationPersistenceService;
import me.nakilex.levelplugin.cooking.runtime.ActiveCookingSessionRegistry;
import me.nakilex.levelplugin.cooking.runtime.PlacedCookingWorkstation;
import me.nakilex.levelplugin.cooking.service.CookingSessionService;
import me.nakilex.levelplugin.cooking.service.CookingWorkstationMatcher;
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
import org.bukkit.inventory.ItemStack;

/** Handles runtime placement tracking and recipe GUI access for cooking workstations. */
public class CookingWorkstationListener implements Listener {
    private final Main plugin;
    private final CookingWorkstationMatcher workstationMatcher;
    private final PlacedCookingWorkstationRegistry placedWorkstations;
    private final ActiveCookingSessionRegistry activeSessions;
    private final CookingRecipeSelectionGUI recipeSelectionGUI;
    private final CookingSessionService sessionService;
    private final CookingWorkstationPersistenceService persistenceService;

    public CookingWorkstationListener(
            Main plugin,
            CookingWorkstationMatcher workstationMatcher,
            PlacedCookingWorkstationRegistry placedWorkstations,
            ActiveCookingSessionRegistry activeSessions,
            CookingRecipeSelectionGUI recipeSelectionGUI,
            CookingSessionService sessionService,
            CookingWorkstationPersistenceService persistenceService
    ) {
        this.plugin = plugin;
        this.workstationMatcher = workstationMatcher;
        this.placedWorkstations = placedWorkstations;
        this.activeSessions = activeSessions;
        this.recipeSelectionGUI = recipeSelectionGUI;
        this.sessionService = sessionService;
        this.persistenceService = persistenceService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        ItemStack placedItem = event.getItemInHand();
        workstationMatcher.findMatchingPlacedWorkstation(block.getType(), placedItem).ifPresent(type -> {
            PlacedCookingWorkstation placed = placedWorkstations.register(block, type, event.getPlayer().getUniqueId());
            persistenceService.save(placedWorkstations.all());
            plugin.getLogger().info("[Cooking] Registered placed workstation '" + type.id()
                    + "' at " + placed.locationKey() + " by " + event.getPlayer().getName() + ".");
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        placedWorkstations.unregister(event.getBlock()).ifPresent(placed -> {
            sessionService.cancelSessionByWorkstation(placed.locationKey());
            persistenceService.save(placedWorkstations.all());
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

            // Check this before the active-session branch. A successful mini-game click can complete
            // the stage/session and then still arrive here through another interaction path.
            // In that case the same physical click must not open the recipe book or advance
            // into the next stage.
            if (sessionService.consumeRecipeBookOpenSuppression(event.getPlayer())) {
                return;
            }

            // Important: during an active cooking session, right-clicking the workstation is gameplay input.
            // Do not let the workstation GUI/"already active" path swallow the click before the mini-game sees it.
            if (activeSessions.getByWorkstation(placed.locationKey()).isPresent()) {
                plugin.getLogger().info("[CookingMiniGameDebug] workstation-click routed as active gameplay input"
                        + " player=" + event.getPlayer().getName()
                        + " workstation=" + placed.locationKey()
                        + " action=" + event.getAction()
                        + " hand=" + event.getHand()
                        + " held=" + event.getPlayer().getInventory().getItemInMainHand().getType());
                sessionService.insertHeldIngredient(event.getPlayer(), placed,
                        event.getPlayer().getInventory().getItemInMainHand(), clicked.getLocation());
                return;
            }

            if (placed.type().permissionNode().isPresent()
                    && !event.getPlayer().hasPermission(placed.type().permissionNode().get())) {
                ChatMessageUtil.send(event.getPlayer(), ChatMessageUtil.MessageType.WARNING,
                        "You do not have permission to use this cooking workstation.");
                return;
            }
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
