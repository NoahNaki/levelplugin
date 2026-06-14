package me.nakilex.levelplugin.cooking;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cooking.config.CookingConfigLoader;
import me.nakilex.levelplugin.cooking.config.CookingConfigLoader.CookingConfigData;
import me.nakilex.levelplugin.cooking.listener.CookingWorkstationListener;
import me.nakilex.levelplugin.cooking.model.CookingWorkstationType;
import me.nakilex.levelplugin.cooking.registry.CookingRecipeRegistry;
import me.nakilex.levelplugin.cooking.registry.CookingWorkstationRegistry;
import me.nakilex.levelplugin.cooking.runtime.PlacedCookingWorkstationRegistry;

/** Foundation module for config-backed cooking data and workstation placement tracking. */
public class CookingModule {
    private final Main plugin;
    private final CookingConfigLoader configLoader;
    private final CookingRecipeRegistry recipeRegistry = new CookingRecipeRegistry();
    private final CookingWorkstationRegistry workstationRegistry = new CookingWorkstationRegistry();
    private final PlacedCookingWorkstationRegistry placedWorkstationRegistry = new PlacedCookingWorkstationRegistry();

    public CookingModule(Main plugin) {
        this.plugin = plugin;
        this.configLoader = new CookingConfigLoader(plugin);
        plugin.getServer().getPluginManager().registerEvents(
                new CookingWorkstationListener(plugin, workstationRegistry, placedWorkstationRegistry),
                plugin);
    }

    public void load() {
        CookingConfigData data = configLoader.load();
        recipeRegistry.replaceAll(data.recipes());
        workstationRegistry.replaceAll(data.workstations());
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
