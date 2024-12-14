package me.nakilex.levelplugin;

import me.nakilex.levelplugin.commands.*;
import me.nakilex.levelplugin.items.ItemManager;
import me.nakilex.levelplugin.listeners.*;
import me.nakilex.levelplugin.managers.StatsManager;
import me.nakilex.levelplugin.tasks.ActionBarTask;
import me.nakilex.levelplugin.tasks.ManaRegenTask;
import me.nakilex.levelplugin.tasks.WeaponCheckTask;
import me.nakilex.levelplugin.ui.ClassMenuListener;
import org.bukkit.plugin.java.JavaPlugin;
import me.nakilex.levelplugin.managers.LevelManager;
import me.nakilex.levelplugin.ui.StatsMenuListener;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Main extends JavaPlugin {

    private static Main instance;
    private LevelManager levelManager;
    private FileConfiguration mobConfig;

    @Override
    public void onEnable() {
        instance = this;
        createMobConfig();

        levelManager = new LevelManager(this);
        StatsManager.getInstance().setLevelManager(levelManager);
        new ActionBarTask().runTaskTimer(this, 20L, 20L);
        new WeaponCheckTask().runTaskTimer(this, 20L, 20L); // every 1 second
        new ManaRegenTask().runTaskTimer(this, 20L, 20L);
        new ItemManager(this);


        // Register listeners
        getServer().getPluginManager().registerEvents(new MobDamageListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerKillListener(levelManager, mobConfig), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(levelManager), this);
        getServer().getPluginManager().registerEvents(new StatsMenuListener(), this);
        getServer().getPluginManager().registerEvents(new StatsEffectListener(), this);
        getServer().getPluginManager().registerEvents(new ArmorEquipListener(), this);
        getServer().getPluginManager().registerEvents(new WeaponHeldListener(), this);





        // Register commands
        getCommand("addpoints").setExecutor(new AddPointsCommand());
        getCommand("addxp").setExecutor(new AddXPCommand(levelManager));
        getCommand("stats").setExecutor(new StatsCommand());
        getCommand("additem").setExecutor(new AddItemCommand());
        getCommand("setlevel").setExecutor(new SetLevelCommand(this));
        getCommand("class").setExecutor(new ClassCommand());
        getServer().getPluginManager().registerEvents(new ClassMenuListener(), this);



        getLogger().info("LevelPlugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("LevelPlugin has been disabled!");
    }

    public static Main getInstance() {
        return instance;
    }

    public LevelManager getLevelManager() {
        return levelManager;
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
