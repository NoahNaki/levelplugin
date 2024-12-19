package me.nakilex.levelplugin.economy;

import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class EconomyManager {

    private Plugin plugin;
    private File balanceFile;
    private FileConfiguration balanceConfig;

    public EconomyManager(Plugin plugin) {
        this.plugin = plugin;
        loadBalances();
    }

    public void loadBalances() {
        balanceFile = new File(plugin.getDataFolder(), "balances.yml");
        if(!balanceFile.exists()) {
            try {
                balanceFile.createNewFile();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        balanceConfig = YamlConfiguration.loadConfiguration(balanceFile);
    }

    public void saveBalances() {
        try {
            balanceConfig.save(balanceFile);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public int getBalance(Player player) {
        String path = "balances." + player.getUniqueId().toString();
        return balanceConfig.getInt(path, 0);
    }

    public void setBalance(Player player, int amount) {
        String path = "balances." + player.getUniqueId().toString();
        balanceConfig.set(path, amount);
        saveBalances();
    }

    public void addCoins(Player player, int amount) {
        int current = getBalance(player);
        setBalance(player, current + amount);
    }

    public void deductCoins(Player player, int amount) {
        int current = getBalance(player);
        if (current >= amount) {
            setBalance(player, current - amount);
        } else {
            throw new IllegalArgumentException("Not enough coins to deduct!");
        }
    }

}
