package me.nakilex.levelplugin.cooking;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cooking.config.CookingConfigLoader;
import me.nakilex.levelplugin.cooking.config.CookingConfigLoader.CookingConfigData;
import me.nakilex.levelplugin.cooking.gui.CookingRecipeSelectionGUI;
import me.nakilex.levelplugin.cooking.listener.CookingIngredientListener;
import me.nakilex.levelplugin.cooking.listener.CookingWorkstationListener;
import me.nakilex.levelplugin.cooking.model.CookingWorkstationType;
import me.nakilex.levelplugin.cooking.registry.CookingRecipeRegistry;
import me.nakilex.levelplugin.cooking.registry.CookingWorkstationRegistry;
import me.nakilex.levelplugin.cooking.runtime.ActiveCookingSessionRegistry;
import me.nakilex.levelplugin.cooking.runtime.PlacedCookingWorkstationRegistry;
import me.nakilex.levelplugin.cooking.service.CookingSessionService;
import me.nakilex.levelplugin.cooking.service.CookingWorkstationMatcher;

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

    public CookingModule(Main plugin) {
        this.plugin = plugin;
        this.configLoader = new CookingConfigLoader(plugin);
        this.sessionService = new CookingSessionService(plugin, recipeRegistry, activeSessionRegistry, placedWorkstationRegistry);
        this.workstationMatcher = new CookingWorkstationMatcher(workstationRegistry);
        this.recipeSelectionGUI = new CookingRecipeSelectionGUI(recipeRegistry, sessionService);
        plugin.getServer().getPluginManager().registerEvents(
                new CookingWorkstationListener(plugin, workstationMatcher, placedWorkstationRegistry, activeSessionRegistry, recipeSelectionGUI, sessionService),
                plugin);
        plugin.getServer().getPluginManager().registerEvents(recipeSelectionGUI, plugin);
        plugin.getServer().getPluginManager().registerEvents(
                new CookingIngredientListener(placedWorkstationRegistry, activeSessionRegistry, sessionService),
                plugin);
    }

    public void load() {
        CookingConfigData data = configLoader.load();
        recipeRegistry.replaceAll(data.recipes());
        workstationRegistry.replaceAll(data.workstations(), plugin.getLogger());
        logMissingRecipeReferences();
        plugin.getLogger().info("[Cooking] Loaded " + recipeRegistry.size() + " recipes and "
                + workstationRegistry.size() + " workstation types.");
    }

    public void reload() {
        load();
    }

    public CookingRecipeRegistry recipes() {
        return recipeRegistry;
    }

    public CookingWorkstationRegistry workstations() {
        return workstationRegistry;
    }

    public PlacedCookingWorkstationRegistry placedWorkstations() {
        return placedWorkstationRegistry;
    }

    public ActiveCookingSessionRegistry activeSessions() {
        return activeSessionRegistry;
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
