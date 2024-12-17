package me.nakilex.levelplugin;

import me.nakilex.levelplugin.commands.*;
import me.nakilex.levelplugin.economy.BalanceCommand;
import me.nakilex.levelplugin.items.ItemManager;
import me.nakilex.levelplugin.listeners.*;
import me.nakilex.levelplugin.managers.LevelManager;
import me.nakilex.levelplugin.managers.StatsManager;
import me.nakilex.levelplugin.mob.MobManager;
import me.nakilex.levelplugin.economy.EconomyManager;
import me.nakilex.levelplugin.spells.SpellManager;
import me.nakilex.levelplugin.tasks.ActionBarTask;
import me.nakilex.levelplugin.tasks.ManaRegenTask;
import me.nakilex.levelplugin.tasks.WeaponCheckTask;
import me.nakilex.levelplugin.ui.ClassMenuListener;
import me.nakilex.levelplugin.ui.StatsMenuListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Main plugin class, integrating custom mobs & economy into the existing structure.
 */
public class Main extends JavaPlugin {

    private static Main instance;

    // Existing managers
    private LevelManager levelManager;

    // New managers for custom mob + economy
    private MobManager mobManager;
    private EconomyManager economyManager;

    public FileConfiguration mobConfig; // from createMobConfig() for "mobs.yml"

    @Override
    public void onEnable() {
        instance = this;
        createMobConfig(); // loads "mobs.yml"

        // Existing plugin managers
        levelManager = new LevelManager(this);
        StatsManager.getInstance().setLevelManager(levelManager);

        // Start tasks
        new ActionBarTask().runTaskTimer(this, 20L, 20L);
        new WeaponCheckTask().runTaskTimer(this, 20L, 20L);
        new ManaRegenTask().runTaskTimer(this, 20L, 20L);

        // Items, spells, etc.
        new ItemManager(this);
        new SpellManager(this); // if using separate spells system

        // New: Initialize custom mob system & economy
        mobManager = new MobManager(this); // loads custom mob configs from "mobs.yml"
        economyManager = new EconomyManager(this); // loads/saves balances

        // Register plugin listeners
        getServer().getPluginManager().registerEvents(new MobDamageListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerKillListener(levelManager, mobConfig), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(levelManager), this);
        getServer().getPluginManager().registerEvents(new StatsMenuListener(), this);
        getServer().getPluginManager().registerEvents(new StatsEffectListener(), this);
        getServer().getPluginManager().registerEvents(new ArmorEquipListener(), this);
        getServer().getPluginManager().registerEvents(new WeaponHeldListener(), this);
        getServer().getPluginManager().registerEvents(new ClickComboListener(), this);

        // If you created a new MobDeathListener for custom loot/coins:
        getServer().getPluginManager().registerEvents(new MobDeathListener(mobManager, economyManager), this);

        // Register commands
        getCommand("addpoints").setExecutor(new AddPointsCommand());
        getCommand("addxp").setExecutor(new AddXPCommand(levelManager));
        getCommand("stats").setExecutor(new StatsCommand());
        getCommand("additem").setExecutor(new AddItemCommand());
        getCommand("setlevel").setExecutor(new SetLevelCommand(this));
        getCommand("class").setExecutor(new ClassCommand());

        // New economy/mob commands if needed
        getCommand("balance").setExecutor(new BalanceCommand(economyManager));
        getCommand("addcoins").setExecutor(new AddCoinsCommand(economyManager));
        getCommand("addmob").setExecutor(new AddMobCommand(mobManager)); // spawn custom mobs

        // UI listener
        getServer().getPluginManager().registerEvents(new ClassMenuListener(), this);

        getLogger().info("LevelPlugin (with custom mob & economy) has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save balances if needed
        if(economyManager != null) {
            economyManager.saveBalances();
        }
        getLogger().info("LevelPlugin has been disabled!");
    }

    public static Main getInstance() {
        return instance;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public MobManager getMobManager() {
        return mobManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    private void createMobConfig() {
        File mobFile = new File(getDataFolder(), "mobs.yml");
        if (!mobFile.exists()) {
            saveResource("mobs.yml", false);
        }
        mobConfig = YamlConfiguration.loadConfiguration(mobFile);

        InputStream defaultStream = this.getResource("mobs.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            mobConfig.setDefaults(defaultConfig);
        }
    }
}
