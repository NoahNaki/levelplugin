package me.nakilex.prisonenchants;

import me.nakilex.prisonenchants.effect.EnchantEffectManager;
import me.nakilex.prisonenchants.hook.EdPrisonBridge;
import me.nakilex.prisonenchants.hook.PrisonBridge;
import me.nakilex.prisonenchants.mine.MineRegionResolver;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class PrisonEnchantsPlugin extends JavaPlugin {
    private EnchantEffectManager effectManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        EdPrisonBridge edPrison = new EdPrisonBridge(this);
        PrisonBridge prison = new PrisonBridge(this, edPrison);
        MineRegionResolver mines = new MineRegionResolver(this);
        effectManager = new EnchantEffectManager(this, mines, prison);

        PrisonEnchantsCommand command = new PrisonEnchantsCommand(this, effectManager, edPrison);
        PluginCommand pluginCommand = getCommand("prisonenchants");
        if (pluginCommand == null) {
            throw new IllegalStateException("prisonenchants command is missing from plugin.yml");
        }
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);

        getLogger().info("Loaded Tornado, Black Hole, Meteor Shower and Acid Rain enchants.");
    }

    @Override
    public void onDisable() {
        if (effectManager != null) {
            effectManager.shutdown();
        }
    }
}
