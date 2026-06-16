package me.nakilex.levelplugin.cooking;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cooking.config.CookingConfigLoader;
import me.nakilex.levelplugin.cooking.config.CookingConfigLoader.CookingConfigData;
import me.nakilex.levelplugin.cooking.gui.CookingRecipeSelectionGUI;
import me.nakilex.levelplugin.cooking.listener.CookingIngredientListener;
import me.nakilex.levelplugin.cooking.listener.CookingSessionLifecycleListener;
import me.nakilex.levelplugin.cooking.listener.CookingWorkstationListener;
import me.nakilex.levelplugin.cooking.model.CookingWorkstationType;
import me.nakilex.levelplugin.cooking.persistence.CookingWorkstationPersistenceService;
import me.nakilex.levelplugin.cooking.registry.CookingRecipeRegistry;
import me.nakilex.levelplugin.cooking.registry.CookingWorkstationRegistry;
import me.nakilex.levelplugin.cooking.runtime.ActiveCookingSessionRegistry;
import me.nakilex.levelplugin.cooking.runtime.PlacedCookingWorkstation;
import me.nakilex.levelplugin.cooking.runtime.PlacedCookingWorkstationRegistry;
import me.nakilex.levelplugin.cooking.service.CookingSessionService;
import me.nakilex.levelplugin.cooking.service.CookingWorkstationMatcher;
import me.nakilex.levelplugin.cooking.util.CookingLocationKey;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Foundation module for config-backed cooking data and workstation placement tracking. */
public class CookingModule {
    private final Main plugin;
    private final CookingConfigLoader configLoader;
    private final CookingRecipeRegistry recipeRegistry = new CookingRecipeRegistry();
    private final CookingWorkstationRegistry workstationRegistry = new CookingWorkstationRegistry();
    private final PlacedCookingWorkstationRegistry placedWorkstationRegistry = new PlacedCookingWorkstationRegistry();
    private final ActiveCookingSessionRegistry activeSessionRegistry = new ActiveCookingSessionRegistry();
    private final CookingSessionService sessionService;
    private final CookingWorkstationMatcher workstationMatcher;
    private final CookingRecipeSelectionGUI recipeSelectionGUI;
    private final CookingWorkstationPersistenceService workstationPersistenceService;

    public CookingModule(Main plugin) {
        this.plugin = plugin;
        this.configLoader = new CookingConfigLoader(plugin);
        this.workstationPersistenceService = new CookingWorkstationPersistenceService(plugin);
        this.sessionService = new CookingSessionService(plugin, recipeRegistry, activeSessionRegistry, placedWorkstationRegistry);
        this.workstationMatcher = new CookingWorkstationMatcher(workstationRegistry);
        this.recipeSelectionGUI = new CookingRecipeSelectionGUI(recipeRegistry, sessionService);
        plugin.getServer().getPluginManager().registerEvents(
                new CookingWorkstationListener(plugin, workstationMatcher, placedWorkstationRegistry, activeSessionRegistry,
                        recipeSelectionGUI, sessionService, workstationPersistenceService),
                plugin);
        plugin.getServer().getPluginManager().registerEvents(recipeSelectionGUI, plugin);
        plugin.getServer().getPluginManager().registerEvents(
                new CookingIngredientListener(placedWorkstationRegistry, activeSessionRegistry, sessionService),
                plugin);
        plugin.getServer().getPluginManager().registerEvents(
                new CookingSessionLifecycleListener(activeSessionRegistry, sessionService),
                plugin);
    }

    public void load() {
        CookingConfigData data = configLoader.load();
        recipeRegistry.replaceAll(data.recipes());
        workstationRegistry.replaceAll(data.workstations(), plugin.getLogger());
        logMissingRecipeReferences();
        workstationPersistenceService.load(placedWorkstationRegistry, workstationRegistry);
        plugin.getLogger().info("[Cooking] Loaded " + recipeRegistry.size() + " recipes and "
                + workstationRegistry.size() + " workstation types.");
    }

    public void reload() {
        sessionService.shutdownAndRefundAll();
        load();
    }

    public void shutdown() {
        sessionService.shutdownAndRefundAll();
        workstationPersistenceService.save(placedWorkstationRegistry.all());
    }

    public CookingRecipeRegistry recipes() {
        return recipeRegistry;
    }

    public CookingWorkstationRegistry workstations() {
        return workstationRegistry;
    }

    public void openFurnitureWorkstation(Player player, Location furnitureLocation, String furnitureId) {
        if (player == null || furnitureLocation == null || furnitureId == null || furnitureId.isBlank()) {
            return;
        }
        CookingWorkstationType type = workstationRegistry.findByNexoItemId(furnitureId).orElse(null);
        if (type == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "This furniture is not configured as a cooking workstation.");
            return;
        }
        CookingLocationKey locationKey = CookingLocationKey.of(furnitureLocation);
        PlacedCookingWorkstation placed = placedWorkstationRegistry.find(locationKey)
                .orElseGet(() -> placedWorkstationRegistry.registerTransient(furnitureLocation, type));
        if (activeSessionRegistry.getByWorkstation(locationKey).isPresent()) {
            sessionService.insertHeldIngredient(player, placed, player.getInventory().getItemInMainHand(), furnitureLocation);
            return;
        }
        if (type.permissionNode().isPresent() && !player.hasPermission(type.permissionNode().get())) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "You do not have permission to use this cooking workstation.");
            return;
        }
        if (activeSessionRegistry.getByPlayer(player.getUniqueId()).isPresent()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "You already have an active cooking session.");
            return;
        }
        recipeSelectionGUI.open(player, placed);
    }

    public PlacedCookingWorkstationRegistry placedWorkstations() {
        return placedWorkstationRegistry;
    }

    public ActiveCookingSessionRegistry activeSessions() {
        return activeSessionRegistry;
    }

    public CookingSessionService sessionService() {
        return sessionService;
    }

    private void logMissingRecipeReferences() {
        for (CookingWorkstationType workstation : workstationRegistry.all()) {
            for (String recipeId : workstation.recipeIds()) {
                if (!recipeRegistry.contains(recipeId)) {
                    plugin.getLogger().warning("[Cooking] Workstation '" + workstation.id()
                            + "' references missing recipe '" + recipeId + "'.");
                }
            }
        }
    }
}
