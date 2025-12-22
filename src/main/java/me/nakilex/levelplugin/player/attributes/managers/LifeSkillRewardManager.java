package me.nakilex.levelplugin.player.attributes.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import me.nakilex.levelplugin.player.farming.managers.FarmingManager;
import me.nakilex.levelplugin.player.mining.managers.MiningManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Centralises life skill reward milestones and claims so both mining and farming
 * can share the same structures. Rewards are defined once per discipline and
 * can be persisted through {@link me.nakilex.levelplugin.player.config.PlayerConfig}.
 */
public class LifeSkillRewardManager {

    private static LifeSkillRewardManager instance;

    private final Map<ToolDiscipline, List<LifeSkillReward>> rewards = new EnumMap<>(ToolDiscipline.class);
    private final Map<ToolDiscipline, Map<UUID, Set<Integer>>> claimed = new EnumMap<>(ToolDiscipline.class);

    private final EconomyManager economyManager;
    private final MiningManager miningManager;
    private final FarmingManager farmingManager;

    public LifeSkillRewardManager(Main plugin) {
        this.economyManager = plugin.getEconomyManager();
        this.miningManager = plugin.getMiningManager();
        this.farmingManager = plugin.getFarmingManager();
        instance = this;

        initialiseRewards();
        for (ToolDiscipline discipline : ToolDiscipline.values()) {
            claimed.put(discipline, new java.util.HashMap<>());
        }
    }

    public static LifeSkillRewardManager getInstance() {
        return instance;
    }

    private void initialiseRewards() {
        rewards.put(ToolDiscipline.FARMING, createRewardList("Farming"));
        rewards.put(ToolDiscipline.MINING, createRewardList("Mining"));
    }

    private List<LifeSkillReward> createRewardList(String skillName) {
        return List.of(
                new LifeSkillReward(1, ChatColor.GOLD + skillName + " Level 1",
                        List.of(ChatColor.GRAY + "Rewards:",
                                ChatColor.GREEN + "•" + ChatColor.GRAY + " Access to the Barn",
                                ChatColor.GREEN + "•" + ChatColor.GRAY + " +100 Coins"),
                        player -> economyManager.addCoins(player, 100, false)),
                new LifeSkillReward(5, ChatColor.GOLD + skillName + " Level 5",
                        List.of(ChatColor.GRAY + "Rewards:",
                                ChatColor.GREEN + "•" + ChatColor.GRAY + " +250 Coins",
                                ChatColor.GREEN + "•" + ChatColor.GRAY + " +2 Max Hearts"),
                        player -> economyManager.addCoins(player, 250, false)),
                new LifeSkillReward(10, ChatColor.GOLD + skillName + " Level 10",
                        List.of(ChatColor.GRAY + "Rewards:",
                                ChatColor.GREEN + "•" + ChatColor.GRAY + " +750 Coins",
                                ChatColor.GREEN + "•" + ChatColor.GRAY + " +4 Wheat Seeds"),
                        player -> {
                            economyManager.addCoins(player, 750, false);
                            player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.WHEAT_SEEDS, 4));
                        }),
                new LifeSkillReward(15, ChatColor.GOLD + skillName + " Level 15",
                        List.of(ChatColor.GRAY + "Rewards:",
                                ChatColor.GREEN + "•" + ChatColor.GRAY + " +1,500 Coins",
                                ChatColor.GREEN + "•" + ChatColor.GRAY + " +1 Rare Fertilizer"),
                        player -> {
                            economyManager.addCoins(player, 1500, false);
                            player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BONE_MEAL, 1));
                        }),
                new LifeSkillReward(20, ChatColor.GOLD + skillName + " Level 20",
                        List.of(ChatColor.GRAY + "Rewards:",
                                ChatColor.GREEN + "•" + ChatColor.GRAY + " +3,000 Coins",
                                ChatColor.GREEN + "•" + ChatColor.GRAY + " +1 Harvest Booster"),
                        player -> economyManager.addCoins(player, 3000, false))
        );
    }

    public List<LifeSkillReward> getRewards(ToolDiscipline discipline) {
        return rewards.getOrDefault(discipline, List.of());
    }

    public boolean isClaimed(UUID uuid, ToolDiscipline discipline, int levelRequired) {
        return claimed.getOrDefault(discipline, Map.of())
                .getOrDefault(uuid, Set.of())
                .contains(levelRequired);
    }

    public void setClaimed(UUID uuid, ToolDiscipline discipline, Set<Integer> claimedLevels) {
        claimed.computeIfAbsent(discipline, d -> new java.util.HashMap<>()).put(uuid, new HashSet<>(claimedLevels));
    }

    public Set<Integer> getClaimed(UUID uuid, ToolDiscipline discipline) {
        return new HashSet<>(claimed.getOrDefault(discipline, Map.of()).getOrDefault(uuid, Set.of()));
    }

    public void saveClaim(UUID uuid, ToolDiscipline discipline, int levelRequired) {
        claimed.computeIfAbsent(discipline, d -> new java.util.HashMap<>())
                .computeIfAbsent(uuid, u -> new HashSet<>())
                .add(levelRequired);
    }

    public boolean claimReward(Player player, ToolDiscipline discipline, LifeSkillReward reward) {
        UUID uuid = player.getUniqueId();
        if (isClaimed(uuid, discipline, reward.levelRequired())) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "You've already claimed this reward.");
            return false;
        }

        int currentLevel = getLevel(discipline, uuid);
        if (currentLevel < reward.levelRequired()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Reach level " + reward.levelRequired() + " to claim this reward.");
            return false;
        }

        reward.rewardAction().accept(player);
        saveClaim(uuid, discipline, reward.levelRequired());
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Reward claimed!");
        return true;
    }

    private int getLevel(ToolDiscipline discipline, UUID uuid) {
        return switch (discipline) {
            case MINING -> miningManager.getLevel(uuid);
            case FARMING -> farmingManager.getLevel(uuid);
        };
    }

    public record LifeSkillReward(int levelRequired, String displayName, List<String> lore, Consumer<Player> rewardAction) {
    }
}
