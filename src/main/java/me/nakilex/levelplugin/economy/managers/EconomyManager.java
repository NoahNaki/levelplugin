package me.nakilex.levelplugin.economy.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.booster.BoosterType;
import me.nakilex.levelplugin.booster.GlobalBoosterManager;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.util.UUID;

import java.io.File;
import java.io.IOException;

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

    public int getBalance(UUID playerId) {
        String path = "balances." + playerId.toString();
        return balanceConfig.getInt(path, 0);
    }

    public void setBalance(Player player, int amount) {
        String path = "balances." + player.getUniqueId().toString();
        balanceConfig.set(path, amount);
        saveBalances();
        updateLeaderboard();
    }

    public void setBalance(UUID playerId, int amount) {
        String path = "balances." + playerId.toString();
        balanceConfig.set(path, amount);
        saveBalances();
        updateLeaderboard();
    }

    public void addCoins(Player player, int amount) {
        addCoins(player, amount, true);
    }

    public void addCoins(Player player, int amount, boolean applyBoost) {
        int boosted = calculateCoinReward(amount, applyBoost);
        int current = getBalance(player);
        setBalance(player, current + boosted);
    }

    public void addCoins(UUID playerId, int amount) {
        addCoins(playerId, amount, true);
    }

    public void addCoins(UUID playerId, int amount, boolean applyBoost) {
        int boosted = calculateCoinReward(amount, applyBoost);
        int current = getBalance(playerId);
        setBalance(playerId, current + boosted);
    }

    public void deductCoins(Player player, int amount) {
        int current = getBalance(player);
        if (current >= amount) {
            setBalance(player, current - amount);
        } else {
            throw new IllegalArgumentException("Not enough coins to deduct!");
        }
    }

    public void deductCoins(UUID playerId, int amount) {
        int current = getBalance(playerId);
        if (current >= amount) {
            setBalance(playerId, current - amount);
        } else {
            throw new IllegalArgumentException("Not enough coins to deduct!");
        }
    }

    private void updateLeaderboard() {
        if (plugin instanceof me.nakilex.levelplugin.Main main) {
            if (main.getLeaderboardManager() != null) {
            }
        }
    }
    /** Access to the underlying balance configuration. */
    public FileConfiguration getBalanceConfig() {
        return balanceConfig;
    }

    public int calculateCoinReward(int amount, boolean applyBoost) {
        return applyBoost ? applyCoinBoost(amount) : amount;
    }

    private int applyCoinBoost(int amount) {
        if (amount <= 0) return amount;
        GlobalBoosterManager manager = Main.getInstance().getBoosterManager();
        if (manager == null) return amount;
        double boosted = amount * manager.getMultiplier(BoosterType.COIN);
        return (int)Math.round(boosted);
    }
}
